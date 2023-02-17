package com.transferwise.common.gaffer.starter;

import com.transferwise.common.baseutils.ExceptionUtils;
import com.transferwise.common.baseutils.jdbc.DataSourceProxyUtils;
import com.transferwise.common.gaffer.jdbc.GafferJtaDataSource;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

@Slf4j
public class GafferJtaDataSourceBeanProcessor implements BeanPostProcessor, Ordered, BeanFactoryAware {

  private BeanFactory beanFactory;

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return ExceptionUtils.doUnchecked(() -> {
      if (!(bean instanceof DataSource)) {
        return bean;
      }

      var dataSource = (DataSource) bean;

      if (dataSource.isWrapperFor(GafferJtaDataSource.class)) {
        // Why add the starter, if service already wrapped it by itself?
        log.warn("Datasource '" + dataSource
            + "' is already wrapped with `DataSourceImpl`. Remove the custom wrapping or `tw-gaffer-jta-starter` dependency.");
        return dataSource;
      }

      if (!dataSource.isWrapperFor(HikariDataSource.class)) {
        throw new IllegalStateException("Only Hikari CP is supported.");
      }

      var hikariDataSource = dataSource.unwrap(HikariDataSource.class);
      var databaseName = hikariDataSource.getPoolName();
      if (databaseName == null) {
        throw new IllegalStateException("Hikari's pool name for a data source `" + dataSource + "' is not set.");
      }

      final var properties = beanFactory.getBean(GafferJtaProperties.class);
      final var databaseProperties = properties.getDatabases().get(databaseName);

      var gafferJtaDataSource = new GafferJtaDataSource(dataSource);
      DataSourceProxyUtils.tieTogether(gafferJtaDataSource, dataSource);
      gafferJtaDataSource.setUniqueName(databaseName);
      gafferJtaDataSource.setRegisterAsMBean(false);

      if (databaseProperties != null) {
        gafferJtaDataSource.setRegisterAsMBean(databaseProperties.isRegisterAsMbean());
        gafferJtaDataSource.setOrder(databaseProperties.getCommitOrder());
        gafferJtaDataSource.setBeforeReleaseAutoCommitStrategy(databaseProperties.getAutoCommitStrategy());
        gafferJtaDataSource.setValidationTimeoutSeconds((int) databaseProperties.getConnectionValidationInterval().toSeconds());
      }

      dataSource = gafferJtaDataSource;

      if (databaseProperties == null || databaseProperties.isInstrumentWithSpringIntegrationAdapter()) {
        var springAdapter = new GafferJtaSpringIntegrationDataSourceAdapter();
        springAdapter.setTargetDataSource(dataSource);
        DataSourceProxyUtils.tieTogether(springAdapter, dataSource);
        dataSource = springAdapter;
      }

      return dataSource;
    });
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1000;
  }
}
