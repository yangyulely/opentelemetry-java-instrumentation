/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.ktor.v2_0;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.ktor.client.HttpClientConfig;
import io.ktor.client.engine.HttpClientEngineConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.ktor.v2_0.client.KtorClientTracing;
import io.opentelemetry.instrumentation.ktor.v2_0.client.KtorClientTracingBuilder;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class HttpClientInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.ktor.client.HttpClient");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor()
            .and(takesArguments(2))
            .and(takesArgument(1, named("io.ktor.client.HttpClientConfig"))),
        this.getClass().getName() + "$ConstructorAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
        @Advice.Argument(1) HttpClientConfig<HttpClientEngineConfig> httpClientConfig) {
      httpClientConfig.install(KtorClientTracing.Companion, new SetupFunction());
    }
  }

  public static class SetupFunction implements Function1<KtorClientTracingBuilder, Unit> {

    @Override
    public Unit invoke(KtorClientTracingBuilder builder) {
      OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
      builder.setOpenTelemetry(openTelemetry);
      builder.setCapturedRequestHeaders(CommonConfig.get().getClientRequestHeaders());
      builder.setCapturedResponseHeaders(CommonConfig.get().getClientResponseHeaders());
      builder.setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods());

      return kotlin.Unit.INSTANCE;
    }
  }
}
