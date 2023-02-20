package com.transferwise.common.gaffer.starter;

import com.transferwise.common.gaffer.ServiceRegistry;
import com.transferwise.common.gaffer.ServiceRegistryHolder;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
public class GafferJtaConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public UserTransaction gafferJtaUserTransaction() {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    return serviceRegistry.getUserTransaction();
  }

  @Bean
  @ConditionalOnMissingBean
  public TransactionManager gafferJtaTransactionManager(GafferJtaProperties properties) {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    serviceRegistry.getConfiguration().setBeforeCommitValidationRequiredTimeMs(properties.getBeforeCommitValidationRequiredTime().toMillis());
    return serviceRegistry.getTransactionManager();
  }

  @Bean
  @ConditionalOnMissingBean
  public JtaTransactionManager gafferJtaJtaTransactionManager(UserTransaction userTransaction, TransactionManager transactionManager) {
    ServiceRegistry serviceRegistry = ServiceRegistryHolder.getServiceRegistry();
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
    jtaTransactionManager.setTransactionSynchronizationRegistry(serviceRegistry.getTransactionSynchronizationRegistry());
    jtaTransactionManager.setAllowCustomIsolationLevels(true);
    return jtaTransactionManager;
  }

  @Bean
  @ConditionalOnMissingBean
  @ConfigurationProperties(prefix = "tw-gaffer-jta.core", ignoreUnknownFields = false)
  public GafferJtaProperties gafferJtaProperties() {
    return new GafferJtaProperties();
  }

  @Bean
  @ConditionalOnMissingBean
  public GafferJtaDataSourceBeanProcessor gafferJtaDataSourceBeanProcessor() {
    return new GafferJtaDataSourceBeanProcessor();
  }
}
