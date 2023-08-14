/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.metro;

import com.sun.xml.ws.api.message.Packet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.javaagent.bootstrap.servlet.ServletContextPath;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;

public final class MetroServerSpanNaming {

  public static void updateServerSpanName(Context context, MetroRequest metroRequest) {
    String spanName = metroRequest.spanName();
    if (spanName == null) {
      return;
    }

    Span serverSpan = LocalRootSpan.fromContextOrNull(context);
    if (serverSpan == null) {
      return;
    }

    Packet packet = metroRequest.packet();
    if (packet.supports(MessageContext.SERVLET_REQUEST)) {
      Object request = packet.get(MessageContext.SERVLET_REQUEST);
      if (request instanceof HttpServletRequest) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String servletPath = httpRequest.getServletPath();
        if (!servletPath.isEmpty()) {
          String pathInfo = httpRequest.getPathInfo();
          if (pathInfo != null) {
            spanName = servletPath + "/" + spanName;
          } else {
            // when pathInfo is null then there is a servlet that is mapped to this exact service
            // servletPath already contains the service name
            String operationName = packet.getWSDLOperation().getLocalPart();
            spanName = servletPath + "/" + operationName;
          }
        }
      }
    }

    serverSpan.updateName(ServletContextPath.prepend(context, spanName));
  }

  private MetroServerSpanNaming() {}
}
