package com.transferwise.common.gaffer.test;

import com.transferwise.common.baseutils.meters.cache.IMeterCache;
import com.transferwise.common.baseutils.meters.cache.MeterCache;
import com.transferwise.common.gaffer.DefaultGafferTransactionManager;
import com.transferwise.common.gaffer.DefaultMetricsTemplate;
import com.transferwise.common.gaffer.GafferJtaProperties;
import com.transferwise.common.gaffer.GafferTransactionManager;
import com.transferwise.common.gaffer.GafferUserTransaction;
import com.transferwise.common.gaffer.MetricsTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.transaction.UserTransaction;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.jta.JtaTransactionManager;

public abstract class BaseTestApplication {

  @Bean
  public MetricsTestHelper metricsTestHelper() {
    return new MetricsTestHelper();
  }

  @Bean
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Bean
  public IMeterCache meterCache(MeterRegistry meterRegistry) {
    return new MeterCache(meterRegistry);
  }

  @Bean
  public MetricsTemplate metricsTemplate(IMeterCache meterCache) {
    return new DefaultMetricsTemplate(meterCache);
  }

  @Bean
  public UserTransaction gafferUserTransaction(GafferTransactionManager gafferTransactionManager) {
    return new GafferUserTransaction(gafferTransactionManager);
  }

  @Bean
  public GafferTransactionManager gafferTransactionManager(MetricsTemplate metricsTemplate, TestClock clock) {
    return new DefaultGafferTransactionManager(new GafferJtaProperties(), metricsTemplate, clock);
  }

  @Bean
  public JtaTransactionManager transactionManager(GafferTransactionManager transactionManager, UserTransaction userTransaction) {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
    jtaTransactionManager.setTransactionSynchronizationRegistry(transactionManager.getTransactionSynchronizationRegistry());
    return jtaTransactionManager;
  }

  @Bean
  public TestClock testClock() {
    return new TestClock();
  }
}
