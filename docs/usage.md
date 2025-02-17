# Usage

## Integrating with the Library
To use the library, first add the `mavenCentral` repository to your `repositories` block in your `build.gradle`:
```groovy
repositories {
  mavenCentral()
}
```
Then, add the `tw-context` library as a dependency in your `dependencies` block in your `build.gradle`:
```groovy
dependencies {
  implementation 'com.transferwise.common:tw-gaffer-jta:<VERSION>'
}
```
> Replace `<VERSION>` with the version of the library you want to use.
> You can also use `tw-gaffer-jta-starter` which autoconfigures some Spring beans.

## Configuration in Spring Boot Service

1. Add the `com.transferwise.common:tw-gaffer-jta-starter` dependency to `runtimeOnly` configuration.
2. Expose all your HikariCP datasource's as beans.

`tw-gaffer-jta-starter` will automatically wrap all data sources and expose them as beans as Gaffer's `GafferJtaDataSource` instances.

If you need to configure the Gaffer datasource's, you can do so by consulting with the `GafferJtaProperties` class.

For example:

```yaml
tw-gaffer-jta:
  core:
    log-exceptions: false
    databases:
      mydb:
        commitOrder: 15
        connectionValidationInterval: 31s
```
