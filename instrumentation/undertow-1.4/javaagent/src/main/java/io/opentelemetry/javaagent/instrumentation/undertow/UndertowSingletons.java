/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.undertow;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteHolder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.javaagent.bootstrap.servlet.AppServerBridge;
import io.opentelemetry.javaagent.bootstrap.undertow.UndertowActiveHandlers;
import io.undertow.server.HttpServerExchange;

public final class UndertowSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.undertow-1.4";

  private static final Instrumenter<HttpServerExchange, HttpServerExchange> INSTRUMENTER;

  static {
    UndertowHttpAttributesGetter httpAttributesGetter = new UndertowHttpAttributesGetter();

    INSTRUMENTER =
        Instrumenter.<HttpServerExchange, HttpServerExchange>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(httpAttributesGetter))
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
            .addAttributesExtractor(
                HttpServerAttributesExtractor.builder(httpAttributesGetter)
                    .setCapturedRequestHeaders(CommonConfig.get().getServerRequestHeaders())
                    .setCapturedResponseHeaders(CommonConfig.get().getServerResponseHeaders())
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
                    .build())
            .addContextCustomizer(HttpRouteHolder.create(httpAttributesGetter))
            .addContextCustomizer(
                (context, request, attributes) -> {
                  // span is ended when counter reaches 0, we start from 2 which accounts for the
                  // handler that started the span and exchange completion listener
                  context = UndertowActiveHandlers.init(context, 2);

                  return new AppServerBridge.Builder()
                      .captureServletAttributes()
                      .recordException()
                      .init(context);
                })
            .addOperationMetrics(HttpServerMetrics.get())
            .buildServerInstrumenter(UndertowExchangeGetter.INSTANCE);
  }

  private static final UndertowHelper HELPER = new UndertowHelper(INSTRUMENTER);

  public static UndertowHelper helper() {
    return HELPER;
  }

  private UndertowSingletons() {}
}
