# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.0.0]

### Changed

* Refactored the classes and initialisation setup away from static classes.

* Made abandoned transactions tracking configurable.

* Migrated from JMX to Micrometer.

### Migration guide

If you used a custom Spring configuration, it is now the time to move to the autoconfiguration provided by `tw-gaffer-jta-starter`.
Check [docs/usage.md](docs/usage.md) for how to set it up.

If there is a reason why `tw-gaffer-jta-starter` can not be used, then you can replicate the configuration in `GafferJtaConfiguration` class yourself.

It is also possible to just autoconfigure the transaction management, but not autoconfigure data sources. This can be achieved with property of
`tw-gaffer-jta.config.post-process-beans: false`.

And then, the necessary Gaffer compatible data sources can be created as follows.

```java
@Bean
public GafferJtaDataSource accountsDataSource(GafferTransactionManager transactionManager) {
  DataSource innerDataSource = createHikariOrWhatEverDataSource();

  GafferJtaDataSource dataSource = new GafferJtaDataSource(transactionManager, "accounts", innerDataSource);
  dataSource.setCommitOrder(1);
  return dataSource;
}
```

## [3.2.0]

### Changed

* Disabling abandoned transactions tracker.

## [3.1.1]

### Changed

* Add support for spring boot 3.4
* Remove support for spring boot 3.2

## [3.1.0] - 2024-07-18

### Changed

* Run tests against Spring Boot 3.3.
* Baseline moved to Spring Boot 3.2. Library is now built and published against Spring Boot 3.2.

## [3.0.3] - 2024-04-05

### Changed

* Use static methods to create BeanPostProcessors.

## [3.0.2] - 2024-02-29

### Changed

- Added support for Spring Boot 3.2.
  - Updated dependencies.

## [3.0.1] - 2023-08-01

### Added

* Support for Spring Boot 3.1

### Bumped

* Build against Spring Boot 3.0.6 --> 3.0.7

## [3.0.0] - 2023-05-06

### Changed

* Moving from `javax` packages to `jakarta`.
  That means we will start supporting JDK 17 and Spring Boot 3 only from now on.
  Cloned `javax` version to `transferwise/tw-gaffer-jta-javax` repository, for case we need to do urgent changes for older services.

* Jakarta Validation is done now programmatically.
  Somehow the `@Validated` annotation based validation considerable slowed down the application startup.

## [2.2.0] - 2023-03-30

### Changed

* `gafferJtaJtaTransactionManager` bean name to `transactionManager`.
  We would like the `tw-gaffer-jta-starter` to be fully autoconfiguring the service. Unfortunately, some Spring Boot starter libraries are
  specifically expecting a bean named `transactionManager`.

### Migration Guide

#### Transaction Manager Bean Name

If you were using hardcoded `gafferJtaJtaTransactionManager` bean name in your service, you have to change it now to `transactionManager`.

## [2.1.0] - 2023-02-14

### Added

* Spring Boot autoconfiguration module `tw-gaffer-jta-starter`.

### Changed

* Connection and DataSource wrappers are now implementing `tw-base-utils`'s wrapper interfaces.
* `com.transferwise.common.gaffer.jdbc.DataSourceImpl` class was renamed to `com.transferwise.common.gaffer.jdbc.GafferJtaDataSource`.

### Migration Guide

#### Auto Configuration for Gradle

It is recommended to use full autoconfiguration, which will set up the transaction manager and instrument all the data sources.

For that

1. Make sure, that all your data sources are exposed as beans.
   If this is not possible, you still need to wrap those data sources into `GafferJtaDataSource`, manually.
2. Remove `tw-gaffer-jta` from `implementation` configuration.
   Unless, you would need manual wrapping of `GafferJtaDataSource`.
3. Remove custom wrappings of `GafferJtaDataSource` / `DataSourceImpl`.
   In Wise context, your data source beans should be just plain `HikariDataSource` instances.
4. Remove the code creating all the beans now defined in the `GafferJtaConfiguration` class.
   In a typical Wise service, it comes down to deleting the whole `TransactionManagerConfiguration` class.
5. Add `tw-gaffer-jta-starter` into `runtimeOnly` configuration.

#### Without Auto Configuration

* Change all `com.transferwise.common.gaffer.jdbc.DataSourceImpl` occurrences to `com.transferwise.common.gaffer.jdbc.GafferJtaDataSource`

## [2.0.0] - 2022-12-13

### Fixed

* Transaction timeout 0 is widely considered as no timeout is applied.
  So we do the same.

  The issue was discovered in one of our service, where one method had `@Transactional(timeout=1)`.
  When Spring exits that method, it sets transaction timeout through `JtaTransactionManager` to `0`, indicating that there
  should not be any timeout for next transactions.

  Gaffer however interpreted this as there is a timeout with duration of 0, and ofc. started to give timeout exceptions for all following
  transactions.

  As a side note, it seems like Spring `JtaTransactionManager` is misbehaving. It would be more logical it to either
  a) restore the timeout value to what it was before entering the method
  b) set it to `-1`, which signal to JTA transaction manager to use default timeout.

### Removed

* JMS Support
  Everyone is using Kafka and also Transactional Outbox pattern. There is not much need to have 1PC
  JTA transactions around database changes and JMS.

### Changed

* Upgraded dependencies to the level of Spring Boot 2.6.

* Created matrix tests for Spring Boot 2.5, 2.6 and 2.7 dependencies.

## [1.5.0] - 2022-05-19

### Changed

* Prevent connection from leaking when any tx associated resource error occurs.

## [1.4.0] - 2021-05-27

### Changed

* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.
