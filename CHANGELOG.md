# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.0] - 2023-02-14

### Added

* Spring Boot auto configuration module `tw-gaffer-jta-starter`.

### Changed

* Connection and DataSource wrappers are now implementing `tw-base-utils`'s wrapper interfaces.

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
