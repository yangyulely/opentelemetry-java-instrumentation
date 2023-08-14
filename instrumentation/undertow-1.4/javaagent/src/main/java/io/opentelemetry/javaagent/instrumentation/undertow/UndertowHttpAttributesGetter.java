/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.undertow;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpServerAttributesGetter;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class UndertowHttpAttributesGetter
    implements HttpServerAttributesGetter<HttpServerExchange, HttpServerExchange> {

  @Override
  public String getHttpRequestMethod(HttpServerExchange exchange) {
    return exchange.getRequestMethod().toString();
  }

  @Override
  public List<String> getHttpRequestHeader(HttpServerExchange exchange, String name) {
    HeaderValues values = exchange.getRequestHeaders().get(name);
    return values == null ? Collections.emptyList() : values;
  }

  @Override
  public Integer getHttpResponseStatusCode(
      HttpServerExchange exchange, HttpServerExchange unused, @Nullable Throwable error) {
    return exchange.getStatusCode();
  }

  @Override
  public List<String> getHttpResponseHeader(
      HttpServerExchange exchange, HttpServerExchange unused, String name) {
    HeaderValues values = exchange.getResponseHeaders().get(name);
    return values == null ? Collections.emptyList() : values;
  }

  @Override
  @Nullable
  public String getUrlScheme(HttpServerExchange exchange) {
    return exchange.getRequestScheme();
  }

  @Nullable
  @Override
  public String getUrlPath(HttpServerExchange exchange) {
    return exchange.getRequestPath();
  }

  @Nullable
  @Override
  public String getUrlQuery(HttpServerExchange exchange) {
    return exchange.getQueryString();
  }

  @Nullable
  @Override
  public String getNetworkProtocolName(
      HttpServerExchange exchange, @Nullable HttpServerExchange unused) {
    String protocol = exchange.getProtocol().toString();
    if (protocol.startsWith("HTTP/")) {
      return "http";
    }
    return null;
  }

  @Nullable
  @Override
  public String getNetworkProtocolVersion(
      HttpServerExchange exchange, @Nullable HttpServerExchange unused) {
    String protocol = exchange.getProtocol().toString();
    if (protocol.startsWith("HTTP/")) {
      return protocol.substring("HTTP/".length());
    }
    return null;
  }

  @Nullable
  @Override
  public String getServerAddress(HttpServerExchange exchange) {
    return exchange.getHostName();
  }

  @Nullable
  @Override
  public Integer getServerPort(HttpServerExchange exchange) {
    return exchange.getHostPort();
  }

  @Override
  @Nullable
  public InetSocketAddress getClientInetSocketAddress(
      HttpServerExchange exchange, @Nullable HttpServerExchange unused) {
    return exchange.getConnection().getPeerAddress(InetSocketAddress.class);
  }

  @Nullable
  @Override
  public InetSocketAddress getServerInetSocketAddress(
      HttpServerExchange exchange, @Nullable HttpServerExchange unused) {
    return exchange.getConnection().getLocalAddress(InetSocketAddress.class);
  }
}
