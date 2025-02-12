package com.transferwise.common.gaffer.test.complextest1.app;

import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.test.BaseTestApplication;
import java.util.Properties;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.transferwise.common.gaffer.test.complextest1.app")
public class TestApplication extends BaseTestApplication {

  protected DataSource baseDataSource() {
    DataSource dataSource = new DataSource();
    dataSource.setMaxActive(Integer.getInteger("maxPoolSize"));
    dataSource.setMaxIdle(Integer.getInteger("maxPoolSize"));
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
  public GafferJtaDataSource clientsDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:clients");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "clients", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Users DataSource
  @Bean
  public GafferJtaDataSource usersDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:users");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "users", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Secrets DataSource
  @Bean
  public GafferJtaDataSource secretsDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl(System.getProperty("database.secrets.url"));
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "secrets", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Accounts DataSource
  @Bean
  public GafferJtaDataSource accountsDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:accounts");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "accounts", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Clients Admin DataSource
  @Bean
  public GafferJtaDataSource clientsAdminDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:clients");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "clientsAdmin", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Users Admin DataSource
  @Bean
  public GafferJtaDataSource usersAdminDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:users");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "usersAdmin", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Secrets Admin DataSource
  @Bean
  public GafferJtaDataSource secretsAdminDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:secrets");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "secretsAdmin", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  // Accounts Admin DataSource
  @Bean
  public GafferJtaDataSource accountsAdminDataSource(GafferTransactionManager transactionManager) {
    DataSource innerDataSource = baseDataSource();
    innerDataSource.setUrl("jdbc:hsqldb:mem:accounts");
    innerDataSource.setUsername("SA");
    innerDataSource.setPassword("");

    GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "accountsAdmin", innerDataSource);
    dataSource.setTargetDataSource(innerDataSource);
    dataSource.init();
    return dataSource;
  }

  @Bean
  public LocalSessionFactoryBean clientsSessionFactory(@Qualifier("clientsDataSource") GafferJtaDataSource clientsDataSource,
      JtaTransactionManager transactionManager) {
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(clientsDataSource);
    sessionFactory.setJtaTransactionManager(transactionManager);
    sessionFactory.setPackagesToScan("com.transferwise.common.gaffer.test.complextest1.app");

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    sessionFactory.setHibernateProperties(hibernateProperties);

    return sessionFactory;
  }
}
