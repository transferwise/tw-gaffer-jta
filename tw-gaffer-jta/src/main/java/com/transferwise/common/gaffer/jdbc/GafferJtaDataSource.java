package com.transferwise.common.gaffer.jdbc;

import com.transferwise.common.baseutils.jdbc.ConnectionProxyUtils;
import com.transferwise.common.baseutils.jdbc.DataSourceProxyUtils;
import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.OrderedResource;
import com.transferwise.common.gaffer.ValidatableResource;
import com.transferwise.common.gaffer.util.DummyXaResource;
import com.transferwise.common.gaffer.util.XaExceptionImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GafferJtaDataSource extends DataSourceWrapper {

  private static AtomicLong idSequence = new AtomicLong();

  @Getter
  @Setter
  private String uniqueName;

  @Getter
  @Setter
  private AutoCommitStrategy beforeReleaseAutoCommitStrategy = AutoCommitStrategy.NONE;

  @Getter
  @Setter
  private int commitOrder = 0;

  @Getter
  @Setter
  private int validationTimeoutSeconds = 15;

  private final GafferTransactionManager gafferTransactionManager;

  private String id;
  private String connectionResourceKey;
  private Tag dataSourceTag;

  private boolean initialized;

  public GafferJtaDataSource(GafferTransactionManager gafferTransactionManager, String uniqueName, DataSource targetDataSource) {
    super(targetDataSource);

    this.gafferTransactionManager = gafferTransactionManager;
    this.uniqueName = uniqueName;

    DataSourceProxyUtils.tieTogether(this, targetDataSource);
  }

  @PostConstruct
  @SuppressFBWarnings("DC")
  public void init() {
    // No need for volatile as we are dealing with 32 bit literal.
    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          if (StringUtils.isEmpty(uniqueName)) {
            throw new IllegalStateException("Unique name is not set.");
          }
          id = GafferJtaDataSource.class + "." + idSequence.incrementAndGet();
          connectionResourceKey = id + ".con";
          this.dataSourceTag = gafferTransactionManager.getMetricsTemplate().createDataSourceNameTag(uniqueName);
        }
      }
      initialized = true;
    }
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
    return new NonTransactionalConnectionImpl(getConnectionFromDataSource(username, password));
  }

  private Connection getConnection0(String username, String password) throws SQLException {
    init();
    var transactional = gafferTransactionManager.getTransactionSynchronizationRegistry().getTransactionStatus() != Status.STATUS_NO_TRANSACTION;

    gafferTransactionManager.getMetricsTemplate().registerConnectionGet(dataSourceTag, transactional);

    if (gafferTransactionManager.getTransactionSynchronizationRegistry().getTransactionStatus() == Status.STATUS_NO_TRANSACTION) {
      return getNonTransactionalConnection(username, password);
    }
    return getTransactionalConnection(username, password);
  }

  private Connection getTransactionalConnection(String username, String password) throws SQLException {
    var con = (TransactionalConnectionImpl) gafferTransactionManager.getTransactionSynchronizationRegistry().getResource(connectionResourceKey);
    if (con == null) {
      try {
        con = new TransactionalConnectionImpl(getConnectionFromDataSource(username, password));
        gafferTransactionManager.getTransactionSynchronizationRegistry().putResource(connectionResourceKey, con);
        XAResource xaResource = new XaResourceImpl(con, getCommitOrder(), getValidationTimeoutSeconds());
        gafferTransactionManager.getTransaction().enlistResource(xaResource);
      } catch (Throwable rethrow) {
        try {
          if (con != null) {
            con.closeConnection();
          }
        } catch (SQLException e) {
          log.error(e.getMessage(), e);
        }
        if (rethrow instanceof SQLException) {
          throw (SQLException) rethrow;
        }
        throw new SQLException(rethrow);
      }
    } else {
      gafferTransactionManager.getMetricsTemplate().registerConnectionReuse(dataSourceTag, true);
    }
    return con;
  }

  private Connection getConnectionFromDataSource(String username, String password) throws SQLException {
    if (username == null) {
      return getTargetDataSource().getConnection();
    }
    return getTargetDataSource().getConnection(username, password);
  }

  private void setAutoCommitFlag(Connection con, boolean atAcquire, boolean transactional, boolean autoCommit) throws SQLException {
    boolean currentAutoCommit = con.getAutoCommit();

    if (currentAutoCommit != autoCommit) {
      gafferTransactionManager.getMetricsTemplate().registerAutoCommitSwitch(dataSourceTag, atAcquire, transactional, autoCommit);

      con.setAutoCommit(autoCommit);
    }
  }

  private void setAutoCommitBeforeRelease(Connection con, boolean transactional, boolean autoCommitOnBorrow) throws SQLException {
    switch (getBeforeReleaseAutoCommitStrategy()) {
      case RESTORE:
        setAutoCommitFlag(con, false, transactional, autoCommitOnBorrow);
        break;
      case TRUE:
        setAutoCommitFlag(con, false, transactional, true);
        break;
      case FALSE:
        setAutoCommitFlag(con, false, transactional, false);
        break;
      default:
    }
  }

  private class TransactionalConnectionImpl extends ConnectionWrapper {

    private final boolean autoCommitOnBorrow;

    @SuppressFBWarnings("CT")
    public TransactionalConnectionImpl(Connection con) throws SQLException {
      super(con);
      autoCommitOnBorrow = con.getAutoCommit();
      setAutoCommitFlag(con, true, true, false);

      ConnectionProxyUtils.tieTogether(this, con);
    }

    public void closeConnection() throws SQLException {
      gafferTransactionManager.getMetricsTemplate().registerConnectionClose(dataSourceTag, true);
      try (Connection con = getTargetConnection()) {
        setAutoCommitBeforeRelease(con, true, autoCommitOnBorrow);
        log.debug("Closing connection for resource '{}'.", getUniqueName());
      }
    }

    @Override
    public void close() {
    }
  }

  private class NonTransactionalConnectionImpl extends ConnectionWrapper {

    private final boolean autoCommitOnBorrow;

    @SuppressFBWarnings("CT")
    public NonTransactionalConnectionImpl(Connection con) throws SQLException {
      super(con);

      autoCommitOnBorrow = con.getAutoCommit();
      setAutoCommitFlag(con, true, false, true);

      ConnectionProxyUtils.tieTogether(this, con);
    }

    public void close() throws SQLException {
      gafferTransactionManager.getMetricsTemplate().registerConnectionClose(dataSourceTag, false);

      try {
        setAutoCommitBeforeRelease(getTargetConnection(), false, autoCommitOnBorrow);
      } finally {
        super.close();
      }
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

}
