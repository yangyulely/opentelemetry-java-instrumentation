# Appender Instrumentation for Logback version 1.0 and higher

This module provides a Logback [appender](https://logback.qos.ch/manual/appenders.html) which
forwards Logback log events to the
[OpenTelemetry Log SDK](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk/logs).

## Quickstart

### Add these dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:io.opentelemetry.instrumentation%20AND%20a:opentelemetry-logback-appender-1.0).

For Maven, add to your `pom.xml` dependencies:

```xml

<dependencies>
  <dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>OPENTELEMETRY_VERSION</version>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

For Gradle, add to your dependencies:

```groovy
runtimeOnly("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:OPENTELEMETRY_VERSION")
```

### Usage

The following demonstrates how you might configure the appender in your `logback.xml` configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>
        %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
      </pattern>
    </encoder>
  </appender>
  <appender name="OpenTelemetry"
            class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
  </appender>

  <root level="INFO">
    <appender-ref ref="console"/>
    <appender-ref ref="OpenTelemetry"/>
  </root>

</configuration>
```

In this example Logback log events will be sent to both the console appender and
the `OpenTelemetryAppender`.

In order to function, `OpenTelemetryAppender` needs access to an `OpenTelemetry` instance. This must
be set programmatically during application startup as follows:

```java
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Application {

  public static void main(String[] args) {
    OpenTelemetrySdk openTelemetrySdk = // Configure OpenTelemetrySdk

    // Find OpenTelemetryAppender in logback configuration and install openTelemetrySdk
    OpenTelemetryAppender.install(openTelemetrySdk);

    // ... proceed with application
  }
}
```

#### Settings for the Logback Appender

Settings can be configured in `logback.xml`, for example:

```xml
<appender name="OpenTelemetry" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
  <captureExperimentalAttributes>true</captureExperimentalAttributes>
  <captureMdcAttributes>*</captureMdcAttributes>
</appender>
```

The available settings are:

| XML Element                     | Type    | Default | Description                                                                                                                                   |
| ------------------------------- | ------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `captureExperimentalAttributes` | Boolean | `false` | Enable the capture of experimental span attributes `thread.name` and `thread.id`.                                                             |
| `captureCodeAttributes`         | Boolean | `false` | Enable the capture of [source code attributes]. Note that capturing source code attributes at logging sites might add a performance overhead. |
| `captureMarkerAttribute`        | Boolean | `false` | Enable the capture of Logback markers as attributes.                                                                                          |
| `captureKeyValuePairAttributes` | Boolean | `false` | Enable the capture of Logback key value pairs as attributes.                                                                                  |
| `captureMdcAttributes`          | String  |         | Comma separated list of MDC attributes to capture. Use the wildcard character `*` to capture all attributes.                                  |

[source code attributes]: https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#source-code-attributes
