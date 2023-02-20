package com.transferwise.common.gaffer.jdbc;

import com.transferwise.common.baseutils.jdbc.ParentAwareDataSourceProxy;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class DataSourceWrapper implements ParentAwareDataSourceProxy {

  private DataSource targetDataSource;
  private DataSource parentDataSource;

  public DataSourceWrapper() {
  }

  public DataSourceWrapper(DataSource targetDataSource) {
    this.targetDataSource = targetDataSource;
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return targetDataSource.getLogWriter();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface == null) {
      return null;
    }
    if (iface.isInstance(this)) {
      return (T) this;
    }
    return targetDataSource.unwrap(iface);
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    targetDataSource.setLogWriter(out);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    if (iface == null) {
      return false;
    }
    return iface.isInstance(this) || targetDataSource.isWrapperFor(iface);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return targetDataSource.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return targetDataSource.getConnection(username, password);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    targetDataSource.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return targetDataSource.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return targetDataSource.getParentLogger();
  }

  @Override
  public DataSource getParentDataSource() {
    return parentDataSource;
  }

  @Override
  public void setParentDataSource(DataSource dataSource) {
    this.parentDataSource = dataSource;
  }

  @Override
  public DataSource getTargetDataSource() {
    return targetDataSource;
  }

  @Override
  public void setTargetDataSource(DataSource dataSource) {
    this.targetDataSource = dataSource;
  }
}
