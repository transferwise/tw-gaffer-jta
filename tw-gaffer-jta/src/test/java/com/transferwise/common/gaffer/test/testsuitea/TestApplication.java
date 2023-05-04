package com.transferwise.common.gaffer.test.testsuitea;

import com.transferwise.common.gaffer.ServiceRegistry;
import com.transferwise.common.gaffer.ServiceRegistryHolder;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.transferwise.common.gaffer.test.TestClock;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@SpringBootApplication
@EnableTransactionManagement
public class TestApplication {

  @Bean
  public UserTransaction gafferUserTransaction() {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    return serviceRegistry.getUserTransaction();
  }

  @Bean
  public TransactionManager gafferTransactionManager() {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    serviceRegistry.getConfiguration().setBeforeCommitValidationRequiredTimeMs(30000);
    return serviceRegistry.getTransactionManager();
  }

  @Bean
  public JtaTransactionManager transactionManager() {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(gafferUserTransaction(), gafferTransactionManager());
    jtaTransactionManager.setTransactionSynchronizationRegistry(serviceRegistry.getTransactionSynchronizationRegistry());
    return jtaTransactionManager;
  }

  @Bean
  public DataSource usersDataSource() {
    var hds = new HikariDataSource();
    hds.setJdbcUrl("jdbc:hsqldb:mem:users");
    hds.setUsername("SA");
    hds.setPassword("");
    hds.setPoolName("users");

    var ds = new GafferJtaDataSource(hds);
    ds.setUniqueName(hds.getPoolName());
    ds.setRegisterAsMBean(false);

    return ds;
  }

  @Bean
  public TimeoutsIntTest timeoutsTest() {
    return new TimeoutsIntTest();
  }

  @Bean
  public TestClock testClock() {
    return new TestClock();
  }
}
