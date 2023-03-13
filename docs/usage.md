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

1. Add the `com.transferwise.common:tw-gaffer-jta-starter` dependency.
2. Expose all your HikariCP datasource's as beans.
3. `tw-gaffer-jta-starter` Will then automatically wrap all data sources and expose them as beans with Gaffer's `GafferJtaDataSource`.
4. If you need to configure the Gaffer datasource's, you can do so using the `GafferJtaProperties` class*.

\* You can do this in two ways:
1. Create a configuration entry in your `application.yml`, which will then cause the bean to be automatically created. For example:
```yaml
tw-gaffer-jta:
  core:
    databases:
      mydb:
        commitOrder: 15
        connectionValidationInterval: 31s
```
2. Create the `GafferJtaProperties` bean yourself.