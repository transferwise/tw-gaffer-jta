package com.transferwise.common.gaffer.jdbc;

import com.transferwise.common.gaffer.OrderedResource;
import com.transferwise.common.gaffer.ServiceRegistry;
import com.transferwise.common.gaffer.ServiceRegistryHolder;
import com.transferwise.common.gaffer.ValidatableResource;
import com.transferwise.common.gaffer.util.DummyXAResource;
import com.transferwise.common.gaffer.util.FormatLogger;
import com.transferwise.common.gaffer.util.MBeanUtil;
import com.transferwise.common.gaffer.util.XAExceptionImpl;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

public class DataSourceImpl extends DataSourceWrapper implements DataSourceMXBean {
    private static final FormatLogger log = new FormatLogger(DataSourceImpl.class);

    private static AtomicLong idSequence = new AtomicLong();
    private String id;
    private String connectionResourceKey;
    private String uniqueName;
    private boolean registerAsMBean = true;

    private int validationTimeoutSeconds = 0;
    private AutoCommitStrategy beforeReleaseAutoCommitStrategy = AutoCommitStrategy.NONE;
    private int order = 0;
    private volatile boolean initialized;

    private final AtomicLong allConnectionGetsCount = new AtomicLong();
    private final AtomicLong bufferedConnectionGetsCount = new AtomicLong();
    private final AtomicLong nonTransactionalConnectionGetsCount = new AtomicLong();
    private final AtomicLong autoCommitSwitchingsCount = new AtomicLong();

    @PostConstruct
    public void init() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized){
                    if (StringUtils.isEmpty(uniqueName)) {
                        throw new IllegalStateException("Unique name is not set.");
                    }
                    id = DataSourceImpl.class + "." + idSequence.incrementAndGet();
                    connectionResourceKey = id + ".con";

                    if (registerAsMBean) {
                        MBeanUtil.registerMBeanQuietly(this,
                                                       "com.transferwise.common.gaffer:type=JdbcDataSource,name=" + uniqueName);
                    }
                    initialized = true;
                }
            }
        }
    }

    public AutoCommitStrategy getBeforeReleaseAutoCommitStrategy() {
        return beforeReleaseAutoCommitStrategy;
    }

    public void setBeforeReleaseAutoCommitStrategy(AutoCommitStrategy beforeReleaseAutoCommitStrategy) {
        this.beforeReleaseAutoCommitStrategy = beforeReleaseAutoCommitStrategy;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setRegisterAsMBean(boolean registerAsMBean) {
        this.registerAsMBean = registerAsMBean;
    }

    public int getValidationTimeoutSeconds() {
        return validationTimeoutSeconds;
    }

    public void setValidationTimeoutSeconds(int validationTimeoutSeconds) {
        this.validationTimeoutSeconds = validationTimeoutSeconds;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection0(null, null);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection0(username, password);
    }

    private Connection getNonTransactionalConnection(String username, String password) throws SQLException {
        log.debug("Connection requested outside of transaction.");
        NonTransactionalConnectionImpl con = new NonTransactionalConnectionImpl(this,
                                                                                getConnectionFromDataSource(username,
                                                                                                            password));
        nonTransactionalConnectionGetsCount.incrementAndGet();
        return con;
    }

    private Connection getConnection0(String username, String password) throws SQLException {
        init();

        ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
        TransactionSynchronizationRegistry registry = serviceRegistry.getTransactionSynchronizationRegistry();
        allConnectionGetsCount.incrementAndGet();
        if (registry.getTransactionStatus() == Status.STATUS_NO_TRANSACTION) {
            return getNonTransactionalConnection(username, password);
        }
        return getTransactionalConnection(serviceRegistry, registry, username, password);
    }

    private Connection getTransactionalConnection(ServiceRegistry serviceRegistry, TransactionSynchronizationRegistry registry, String username, String password)
        throws SQLException {
        TransactionalConnectionImpl con = (TransactionalConnectionImpl) registry.getResource(connectionResourceKey);
        if (con == null) {
            con = new TransactionalConnectionImpl(this, getConnectionFromDataSource(username, password), uniqueName);
            registry.putResource(connectionResourceKey, con);
            XAResource xaResource = new XAResourceImpl(con, order, getValidationTimeoutSeconds());
            serviceRegistry.getTransactionManager().getTransactionImpl().enlistResource(xaResource);
        } else {
            bufferedConnectionGetsCount.incrementAndGet();
        }
        return con;
    }

    private Connection getConnectionFromDataSource(String username, String password) throws SQLException {
        if (username == null) {
            return getDataSource().getConnection();
        }
        return getDataSource().getConnection(username, password);
    }

    private void setAutoCommit(Connection con, boolean autoCommit) throws SQLException {
        boolean currentAutoCommit = con.getAutoCommit();

        if (currentAutoCommit != autoCommit) {
            autoCommitSwitchingsCount.incrementAndGet();
            con.setAutoCommit(autoCommit);
        }
    }

    private void setAutoCommitBeforeRelease(Connection con, boolean autoCommitOnBorrow) throws SQLException {
        switch (getBeforeReleaseAutoCommitStrategy()) {
            case RESTORE:
                setAutoCommit(con, autoCommitOnBorrow);
                break;
            case TRUE:
                setAutoCommit(con, true);
                break;
            case FALSE:
                setAutoCommit(con, false);
                break;
            default:
        }
    }

    private static class TransactionalConnectionImpl extends ConnectionWrapper {
        private boolean autoCommitOnBorrow;
        private DataSourceImpl dataSourceImpl;
        private String resourceUniqueName;

        public TransactionalConnectionImpl(DataSourceImpl dataSourceImpl, Connection con, String resourceUniqueName) throws SQLException {
            super(con);
            this.dataSourceImpl = dataSourceImpl;
            this.resourceUniqueName = resourceUniqueName;
            autoCommitOnBorrow = con.getAutoCommit();
            dataSourceImpl.setAutoCommit(con, false);
        }

        public void closeConnection() throws SQLException {
            Connection con = getConnection();
            dataSourceImpl.setAutoCommitBeforeRelease(con, autoCommitOnBorrow);
            log.debug("Closing connection for resource '{}'.", resourceUniqueName);
            con.close();
        }

        @Override
        public void close() {
        }
    }

    private static class NonTransactionalConnectionImpl extends ConnectionWrapper {
        private boolean autoCommitOnBorrow;
        private DataSourceImpl dataSourceImpl;

        public NonTransactionalConnectionImpl(DataSourceImpl dataSourceImpl, Connection con) throws SQLException {
            super(con);
            this.dataSourceImpl = dataSourceImpl;
            autoCommitOnBorrow = con.getAutoCommit();
            dataSourceImpl.setAutoCommit(con, true);
        }

        public void close() throws SQLException {
            dataSourceImpl.setAutoCommitBeforeRelease(getConnection(), autoCommitOnBorrow);
            super.close();
        }
    }

    private static class XAResourceImpl extends DummyXAResource implements OrderedResource, ValidatableResource {
        private final TransactionalConnectionImpl con;
        private final int order;
        private final int validationTimeoutSeconds;

        public XAResourceImpl(TransactionalConnectionImpl con, int order, int validationTimeoutSeconds) {
            this.con = con;
            this.order = order;
            this.validationTimeoutSeconds = validationTimeoutSeconds;
        }

        @Override
        public void commit(Xid xid, boolean onePhase) throws XAException {
            try {
                try {
                    con.commit();
                } catch (SQLException e) {
                    throw new XAExceptionImpl(XAException.XAER_RMERR, e);
                }
            } finally {
                try {
                    con.closeConnection();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void rollback(Xid xid) throws XAException {
            try {
                try {
                    con.rollback();
                } catch (SQLException e) {
                    throw new XAExceptionImpl(XAException.XAER_RMERR, e);
                }
            } finally {
                try {
                    con.closeConnection();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public boolean isValid() {
            try {
                return con.isValid(validationTimeoutSeconds);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public long getAllConnectionGetsCount() {
        return allConnectionGetsCount.get();
    }

    @Override
    public long getBufferedConnectionGetsCount() {
        return bufferedConnectionGetsCount.get();
    }

    @Override
    public long getNonTransactionalConnectionGetsCount() {
        return nonTransactionalConnectionGetsCount.get();
    }

    @Override
    public long getAutoCommitSwitchingCount() {
        return autoCommitSwitchingsCount.get();
    }

}
