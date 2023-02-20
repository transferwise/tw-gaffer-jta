package com.transferwise.common.gaffer.starter;

import com.transferwise.common.gaffer.jdbc.DataSourceWrapper;
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

    // Default is false anyway, so we don't set that explicitly. To avoid possible round-trips on certain jdbc drivers.
    if (isCurrentSetToReadOnly()) {
      con.setReadOnly(true);
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

  protected boolean isCurrentSetToReadOnly() {
    return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
  }
}
