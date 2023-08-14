# OpenTelemetry Jaeger Exporter Starter

OpenTelemetry Jaeger Exporter Starter is a starter package that includes the opentelemetry-api, opentelemetry-sdk, opentelemetry-extension-annotations, opentelmetry-logging-exporter, opentelemetry-spring-boot-autoconfigurations and spring framework starters required to setup distributed tracing. It also provides the [opentelemetry-exporters-jaeger](https://github.com/open-telemetry/opentelemetry-java/tree/main/exporters/jaeger) artifact and corresponding exporter auto-configuration. Check out [opentelemetry-spring-boot-autoconfigure](../../spring-boot-autoconfigure/README.md#features) for the list of supported libraries and features.

## Quickstart

### Add these dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the latest stable [release](https://search.maven.org/search?q=g:io.opentelemetry).

- Minimum version: `1.1.0`

#### Maven

```xml
<dependencies>
  <dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-jaeger-spring-boot-starter</artifactId>
    <version>OPENTELEMETRY_VERSION</version>
  </dependency>
</dependencies>
```

#### Gradle

```groovy
implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-exporter-starter:OPENTELEMETRY_VERSION")
```

### Starter Guide

Check out [OpenTelemetry Manual Instrumentation](https://opentelemetry.io/docs/instrumentation/java/manual/) to learn more about
using the OpenTelemetry API to instrument your code.
