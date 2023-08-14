# AWS Java SDK v2 Instrumentation

Instrumentation for [AWS Java SDK v2](https://github.com/aws/aws-sdk-java-v2).

## Usage

To instrument all AWS SDK clients include the `opentelemetry-aws-sdk-2.2-autoconfigure` submodule in your classpath.

To register instrumentation only on a specific SDK client, register the interceptor when creating it.

```java
DynamoDbClient client = DynamoDbClient.builder()
  .overrideConfiguration(ClientOverrideConfiguration.builder()
    .addExecutionInterceptor(AwsSdk.newInterceptor()))
    .build())
  .build();
```

## Trace propagation

The AWS SDK instrumentation always injects the trace header into the request
using the [AWS Trace Header](https://docs.aws.amazon.com/xray/latest/devguide/xray-concepts.html#xray-concepts-tracingheader) format.
This format is the only format recognized by AWS managed services, and populating will allow
propagating the trace through them.

Additionally, you can enable an experimental option to use the configured propagator to inject into
message attributes (see [parent README](../../README.md)). This currently supports the following AWS APIs:

- SQS.SendMessage
- SQS.SendMessageBatch
- SNS.Publish
  (SNS.PublishBatch is not supported at the moment because it is not available in the minimum SDK
  version targeted by the instrumentation)

Note that injection will only happen if, after injection, a maximum of 10 attributes is used to not
run over API limitations set by AWS.

If this does not fulfill your use case, perhaps because you are
using the same SDK with a different non-AWS managed service, let us know so we can provide
configuration for this behavior.
