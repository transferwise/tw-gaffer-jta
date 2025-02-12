package com.transferwise.common.gaffer.starter.test;

import com.transferwise.common.baseutils.transactionsmanagement.TransactionsConfiguration;
import com.transferwise.common.gaffer.jdbc.ConnectionWrapper;
import com.transferwise.common.gaffer.jdbc.DataSourceWrapper;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import javax.sql.DataSource;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * This application is configuring a data source with Gaffer transaction manager.
 */
@SpringBootApplication
@Import(TransactionsConfiguration.class)
@AutoConfigureObservability
public class TestApplication {

  // We create our own mock data source, because hsqldb does not support setting read only flag and isolation level correctly.
  @Bean
  public DataSource dataSource() {
    var hds = new HikariDataSource();
    hds.setPoolName("mydb");
    var mockDataSource = new DataSourceWrapper() {
      @Override
      public Connection getConnection() {
        var con = new MockConnection();
        con.setTargetConnection(Mockito.mock(Connection.class));
        return con;
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }
    };
    mockDataSource.setTargetDataSource(Mockito.mock(DataSource.class));
    hds.setDataSource(mockDataSource);

    return hds;
  }

  static class MockConnection extends ConnectionWrapper {

    private int transactionIsolation;
    private boolean readOnly;

    @Override
    public void setTransactionIsolation(int level) {
      this.transactionIsolation = level;
    }

    @Override
    public int getTransactionIsolation() {
      return transactionIsolation;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
      return readOnly;
    }

  }
}
