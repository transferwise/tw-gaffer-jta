package com.transferwise.common.gaffer.starter;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import com.transferwise.common.baseutils.meters.cache.MeterCache;
import com.transferwise.common.gaffer.DefaultGafferTransactionManager;
import com.transferwise.common.gaffer.DefaultMetricsTemplate;
import com.transferwise.common.gaffer.GafferJtaProperties;
import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.GafferUserTransaction;
import com.transferwise.common.gaffer.MetricsTemplate;
import com.transferwise.common.gaffer.util.Clock;
import com.transferwise.common.gaffer.util.MonotonicClock;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
public class GafferJtaConfiguration {

  @Bean
  @ConditionalOnMissingBean(IMeterCache.class)
  public IMeterCache twDefaultMeterCache(MeterRegistry meterRegistry) {
    return new MeterCache(meterRegistry);
  }

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  public MonotonicClock gafferJtaClock() {
    return new MonotonicClock();
  }

  @Bean
  @ConditionalOnMissingBean(MetricsTemplate.class)
  public DefaultMetricsTemplate gafferJtaMetricsTemplate(IMeterCache meterCache) {
    return new DefaultMetricsTemplate(meterCache);
  }

  @Bean
  @ConditionalOnMissingBean(TransactionManager.class)
  public DefaultGafferTransactionManager gafferJtaTransactionManager(GafferJtaProperties gafferJtaProperties, MetricsTemplate metricsTemplate,
      Clock clock) {
    return new DefaultGafferTransactionManager(gafferJtaProperties, metricsTemplate, clock);
  }

  @Bean
  @ConditionalOnBean(GafferTransactionManager.class)
  @ConditionalOnMissingBean(UserTransaction.class)
  public UserTransaction gafferJtaUserTransaction(GafferTransactionManager gafferTransactionManager) {
    return new GafferUserTransaction(gafferTransactionManager);
  }

  @Bean("transactionManager")
  @ConditionalOnMissingBean
  public JtaTransactionManager gafferJtaJtaTransactionManager(UserTransaction userTransaction, GafferTransactionManager transactionManager) {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
    jtaTransactionManager.setTransactionSynchronizationRegistry(transactionManager.getTransactionSynchronizationRegistry());
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
  @ConditionalOnProperty(value = "tw-gaffer-jta.config.post-process-beans", havingValue = "true", matchIfMissing = true)
  public static GafferJtaDataSourceBeanProcessor gafferJtaDataSourceBeanProcessor() {
    return new GafferJtaDataSourceBeanProcessor();
  }

  @Bean
  @ConditionalOnMissingBean(EnvironmentValidator.class)
  public DefaultEnvironmentValidator gafferJtaEnvironmentValidator() {
    return new DefaultEnvironmentValidator();
  }

}
