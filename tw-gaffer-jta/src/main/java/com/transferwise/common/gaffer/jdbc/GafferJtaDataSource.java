package com.transferwise.common.gaffer.jdbc;

import com.transferwise.common.baseutils.jdbc.ConnectionProxyUtils;
import com.transferwise.common.baseutils.jdbc.DataSourceProxyUtils;
import com.transferwise.common.gaffer.OrderedResource;
import com.transferwise.common.gaffer.ServiceRegistry;
import com.transferwise.common.gaffer.ServiceRegistryHolder;
import com.transferwise.common.gaffer.ValidatableResource;
import com.transferwise.common.gaffer.util.DummyXaResource;
import com.transferwise.common.gaffer.util.FormatLogger;
import com.transferwise.common.gaffer.util.MBeanUtil;
import com.transferwise.common.gaffer.util.XaExceptionImpl;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.apache.commons.lang3.StringUtils;

public class GafferJtaDataSource extends DataSourceWrapper implements DataSourceManagedBean {

  private static final FormatLogger log = new FormatLogger(GafferJtaDataSource.class);

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
  private final AtomicLong closedConnectionsCount = new AtomicLong();

  public GafferJtaDataSource() {

  }

  public GafferJtaDataSource(DataSource targetDataSource) {
    super(targetDataSource);

    DataSourceProxyUtils.tieTogether(this, targetDataSource);
  }

  @PostConstruct
  public void init() {
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          if (StringUtils.isEmpty(uniqueName)) {
            throw new IllegalStateException("Unique name is not set.");
          }
          id = GafferJtaDataSource.class + "." + idSequence.incrementAndGet();
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

  public int getOrder() {
    return order;
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
    NonTransactionalConnectionImpl con = new NonTransactionalConnectionImpl(this, getConnectionFromDataSource(username, password));
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

  private Connection getTransactionalConnection(ServiceRegistry serviceRegistry, TransactionSynchronizationRegistry registry, String username,
      String password)
      throws SQLException {
    TransactionalConnectionImpl con = (TransactionalConnectionImpl) registry.getResource(connectionResourceKey);
    if (con == null) {
      try {
        con = new TransactionalConnectionImpl(this, getConnectionFromDataSource(username, password), uniqueName);
        registry.putResource(connectionResourceKey, con);
        XAResource xaResource = new XaResourceImpl(con, order, getValidationTimeoutSeconds());
        serviceRegistry.getTransactionManager().getTransactionImpl().enlistResource(xaResource);
      } catch (Throwable rethrow) {
        try {
          if (con != null) {
            con.closeConnection();
          }
        } catch (SQLException e) {
          log.error(e.getMessage(), e);
        }
        throw rethrow;
      }
    } else {
      bufferedConnectionGetsCount.incrementAndGet();
    }
    return con;
  }

  private Connection getConnectionFromDataSource(String username, String password) throws SQLException {
    if (username == null) {
      return getTargetDataSource().getConnection();
    }
    return getTargetDataSource().getConnection(username, password);
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
    private GafferJtaDataSource gafferJtaDataSource;
    private String resourceUniqueName;

    public TransactionalConnectionImpl(GafferJtaDataSource gafferJtaDataSource, Connection con, String resourceUniqueName) throws SQLException {
      super(con);
      this.gafferJtaDataSource = gafferJtaDataSource;
      this.resourceUniqueName = resourceUniqueName;
      autoCommitOnBorrow = con.getAutoCommit();
      gafferJtaDataSource.setAutoCommit(con, false);

      ConnectionProxyUtils.tieTogether(this, con);
    }

    public void closeConnection() throws SQLException {
      try (Connection con = getTargetConnection()) {
        gafferJtaDataSource.setAutoCommitBeforeRelease(con, autoCommitOnBorrow);
        log.debug("Closing connection for resource '{}'.", resourceUniqueName);
      }
      gafferJtaDataSource.closedConnectionsCount.incrementAndGet();
    }

    @Override
    public void close() {
    }
  }

  private static class NonTransactionalConnectionImpl extends ConnectionWrapper {

    private boolean autoCommitOnBorrow;
    private GafferJtaDataSource gafferJtaDataSource;

    public NonTransactionalConnectionImpl(GafferJtaDataSource gafferJtaDataSource, Connection con) throws SQLException {
      super(con);
      this.gafferJtaDataSource = gafferJtaDataSource;
      autoCommitOnBorrow = con.getAutoCommit();
      gafferJtaDataSource.setAutoCommit(con, true);

      ConnectionProxyUtils.tieTogether(this, con);
    }

    public void close() throws SQLException {
      try {
        gafferJtaDataSource.setAutoCommitBeforeRelease(getTargetConnection(), autoCommitOnBorrow);
      } finally {
        super.close();
      }
      gafferJtaDataSource.closedConnectionsCount.incrementAndGet();
    }
  }

  private static class XaResourceImpl extends DummyXaResource implements OrderedResource, ValidatableResource {

    private final TransactionalConnectionImpl con;
    private final int order;
    private final int validationTimeoutSeconds;

    public XaResourceImpl(TransactionalConnectionImpl con, int order, int validationTimeoutSeconds) {
      this.con = con;
      this.order = order;
      this.validationTimeoutSeconds = validationTimeoutSeconds;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      try {
        try {
          con.setAutoCommit(true);
          //con.commit();
        } catch (SQLException e) {
          throw new XaExceptionImpl(XAException.XAER_RMERR, e);
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
          throw new XaExceptionImpl(XAException.XAER_RMERR, e);
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

  public long getClosedConnectionsCount() {
    return closedConnectionsCount.get();
  }

}
