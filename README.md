# Gaffer 1 phase commit JTA.

![Apache 2](https://img.shields.io/hexpm/l/plug.svg)
![Java 11](https://img.shields.io/badge/Java-1.8-blue.svg)
![Maven Central](https://badgen.net/maven/v/maven-central/com.transferwise.common/tw-gaffer-jta)

Meant to replace default Spring Transaction Manager mainly for performance reasons. But also allows to write pool-of-1-compatible
code and reduces the probability of half done 1PC scenarios.


## Configuration in Spring
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

## DataSource configuration:
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

## Hibernate/JPA configuration:
```java
vendorProperties.put("hibernate.transaction.jta.platform", new SpringJtaPlatform(jtaTransactionManager));
```

## License
Copyright 2021 TransferWise Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
