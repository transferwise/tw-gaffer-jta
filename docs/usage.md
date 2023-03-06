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

Add the `com.transferwise.common:tw-gaffer-jta-starter` dependency.

It will create the necessary transaction manager objects and wrap all data sources exposed as beans with Gaffer's `GafferJtaDataSource`.

You can configure each datasource via `GafferJtaProperties` class.