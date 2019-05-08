#Gaffer 1 phase commit JTA.

Meant to replace default Spring Transaction Manager mainly for performance reasons. But also allows to write pool-of-1-compatible
code and reduces the probability of half done 1PC scenarios.


##Configuration in Spring
Typical usage:

Transaction Manager configuration:
```java
@Configuration
@EnableTransactionManagement
public class TransactionManagerConfiguration {
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
}
```

DataSource configuration:
```java
    @Bean
	@Override
	public DataSource getJtaDataSource() {
		DataSourceImpl dataSourceImpl = new DataSourceImpl();
		dataSourceImpl.setUniqueName("bssDb");
		dataSourceImpl.setDataSource(createHikariDataSource("bss"));
		dataSourceImpl.setRegisterAsMBean(false);
		dataSourceImpl.setOrder(1);

		return dataSourceImpl;
	}
```

Hibernate/JPA configuration:
```java
vendorProperties.put("hibernate.transaction.jta.platform", new SpringJtaPlatform(jtaTransactionManager));
```