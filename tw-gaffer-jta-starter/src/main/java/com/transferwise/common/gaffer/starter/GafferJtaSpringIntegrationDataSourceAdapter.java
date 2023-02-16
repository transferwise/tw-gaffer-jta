package com.transferwise.common.gaffer.starter;

import com.transferwise.common.gaffer.jdbc.DataSourceWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class GafferJtaSpringIntegrationDataSourceAdapter extends DataSourceWrapper {

  @Override
  public Connection getConnection() throws SQLException {
    return getConnection(null, null);
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection con = super.getConnection(username, password);
    Boolean readOnlyToUse = getCurrentReadOnlyFlag();
    if (readOnlyToUse != null) {
      con.setReadOnly(readOnlyToUse);
    }

    Integer isolationLevelToUse = getCurrentIsolationLevel();
    if (isolationLevelToUse != null) {
      con.setTransactionIsolation(isolationLevelToUse);
    }
    return con;
  }

  protected Integer getCurrentIsolationLevel() {
    return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
  }

  @SuppressFBWarnings("NP")
  protected Boolean getCurrentReadOnlyFlag() {
    boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    return (txReadOnly ? Boolean.TRUE : null);
  }
}
