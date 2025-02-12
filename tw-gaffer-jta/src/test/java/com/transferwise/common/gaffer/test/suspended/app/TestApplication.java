package com.transferwise.common.gaffer.test.suspended.app;

import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.test.BaseTestApplication;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@AutoConfigureObservability
public class TestApplication extends BaseTestApplication {

  protected DataSource baseDataSource() {
    DataSource dataSource = new DataSource();
    dataSource.setMaxActive(2);
    dataSource.setMaxIdle(2);
    dataSource.setMinIdle(0);
    dataSource.setMaxWait(10000);
    dataSource.setMaxAge(3600000);
    dataSource.setInitialSize(0);
    dataSource.setLogAbandoned(true);
    dataSource.setSuspectTimeout(10);
    dataSource.setLogValidationErrors(true);
    dataSource.setFairQueue(true);
    dataSource.setTimeBetweenEvictionRunsMillis(10000);
    dataSource.setMinEvictableIdleTimeMillis(10000);
    dataSource.setRemoveAbandoned(true);
    dataSource.setRemoveAbandonedTimeout(600000);
    dataSource.setAbandonWhenPercentageFull(75);
    dataSource.setJmxEnabled(true);
    dataSource.setValidationInterval(5000);
    dataSource.setTestWhileIdle(true);
    dataSource.setTestOnBorrow(true);
    dataSource.setTestOnReturn(true);
    dataSource.setValidationQuery("SELECT 1 FROM DUAL");
    dataSource.setDefaultTransactionIsolation(2);
    dataSource.setJdbcInterceptors("ConnectionState;StatementFinalizer;SlowQueryReport(threshold=5000,maxQueries=5000);ResetAbandonedTimer");
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    return dataSource;
  }

  @Bean
  public DataSource clientsInnerDataSource() {
    DataSource dataSource = baseDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:clients");
    dataSource.setUsername("SA");
    dataSource.setPassword("");
    return dataSource;
  }

  @Bean
  public DataSource logsInnerDataSource() {
    DataSource dataSource = baseDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:logs");
    dataSource.setUsername("SA");
    dataSource.setPassword("");
    return dataSource;
  }

  @Bean
  public GafferJtaDataSource clientsDataSource(GafferTransactionManager gafferTransactionManager,
      @Qualifier("clientsInnerDataSource") DataSource clientsInnerDataSource) {
    return new GafferJtaDataSource(gafferTransactionManager, "clients", clientsInnerDataSource);
  }

  @Bean
  public GafferJtaDataSource logsDataSource(GafferTransactionManager gafferTransactionManager,
      @Qualifier("logsInnerDataSource") DataSource logsInnerDataSource) {
    return new GafferJtaDataSource(gafferTransactionManager, "logs", logsInnerDataSource);
  }
}
