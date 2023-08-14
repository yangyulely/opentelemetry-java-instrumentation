/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.httpurlconnection;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.PeerServiceAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import java.net.HttpURLConnection;

public final class HttpUrlConnectionSingletons {

  private static final Instrumenter<HttpURLConnection, Integer> INSTRUMENTER;

  static {
    HttpUrlHttpAttributesGetter httpAttributesGetter = new HttpUrlHttpAttributesGetter();

    InstrumenterBuilder<HttpURLConnection, Integer> builder =
        Instrumenter.<HttpURLConnection, Integer>builder(
                GlobalOpenTelemetry.get(),
                "io.opentelemetry.http-url-connection",
                HttpSpanNameExtractor.create(httpAttributesGetter))
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
            .addAttributesExtractor(
                HttpClientAttributesExtractor.builder(httpAttributesGetter)
                    .setCapturedRequestHeaders(CommonConfig.get().getClientRequestHeaders())
                    .setCapturedResponseHeaders(CommonConfig.get().getClientResponseHeaders())
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
                    .build())
            .addAttributesExtractor(
                PeerServiceAttributesExtractor.create(
                    httpAttributesGetter, CommonConfig.get().getPeerServiceMapping()))
            .addAttributesExtractor(
                HttpMethodAttributeExtractor.create(
                    CommonConfig.get().getKnownHttpRequestMethods()))
            .addContextCustomizer(
                (context, httpRequestPacket, startAttributes) ->
                    GetOutputStreamContext.init(context))
            .addOperationMetrics(HttpClientMetrics.get());
    if (CommonConfig.get().shouldEmitExperimentalHttpClientMetrics()) {
      builder.addOperationMetrics(HttpClientExperimentalMetrics.get());
    }
    INSTRUMENTER = builder.buildClientInstrumenter(RequestPropertySetter.INSTANCE);
  }

  public static Instrumenter<HttpURLConnection, Integer> instrumenter() {
    return INSTRUMENTER;
  }

  private HttpUrlConnectionSingletons() {}
}
