/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet;

import static io.opentelemetry.javaagent.instrumentation.servlet.v3_0.snippet.ServletOutputStreamInjectionState.initializeInjectionStateIfNeeded;
import static java.util.logging.Level.FINE;

import io.opentelemetry.javaagent.bootstrap.servlet.SnippetInjectingResponseWrapper;
import io.opentelemetry.javaagent.instrumentation.servlet.snippet.SnippetInjectingPrintWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Notes on Content-Length: the snippet length is only added to the content length when injection
 * occurs and the content length was set previously.
 *
 * <p>If the Content-Length is set after snippet injection occurs (either for the first time or is
 * set again for some reason), we intentionally do not add the snippet length, because the
 * application server may be making that call at the end of a request when it sees the request has
 * not been submitted, in which case it is likely using the real length of content that has been
 * written, including the snippet length.
 */
public class Servlet3SnippetInjectingResponseWrapper extends HttpServletResponseWrapper
    implements SnippetInjectingResponseWrapper {

  private static final Logger logger =
      Logger.getLogger(Servlet3SnippetInjectingResponseWrapper.class.getName());

  public static final String FAKE_SNIPPET_HEADER = "FAKE_SNIPPET_HEADER";

  private static final int UNSET = -1;

  // this is for Servlet 3.1 support
  @Nullable
  private static final MethodHandle setContentLengthLongHandler = findSetContentLengthLongMethod();

  private final String snippet;
  private final int snippetLength;

  private long contentLength = UNSET;

  private SnippetInjectingPrintWriter snippetInjectingPrintWriter = null;

  public Servlet3SnippetInjectingResponseWrapper(HttpServletResponse response, String snippet) {
    super(response);
    this.snippet = snippet;
    snippetLength = snippet.length();
  }

  @Override
  public boolean containsHeader(String name) {
    // this function is overridden in order to make sure the response is wrapped
    // but not wrapped twice
    // we don't use req.setAttribute
    // because async requests pass down their attributes, but don't pass down our wrapped response
    // and so we would see the presence of the attribute and think the response was already wrapped
    // when it really is not
    // see also https://docs.oracle.com/javaee/7/api/javax/servlet/AsyncContext.html
    if (name.equals(FAKE_SNIPPET_HEADER)) {
      return true;
    }
    return super.containsHeader(name);
  }

  @Override
  public void setHeader(String name, String value) {
    handleHeader(name, value);
    super.setHeader(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    handleHeader(name, value);
    super.addHeader(name, value);
  }

  private void handleHeader(String name, String value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      try {
        contentLength = Long.parseLong(value);
      } catch (NumberFormatException ex) {
        logger.log(FINE, "Failed to parse the Content-Length header", ex);
      }
    }
  }

  @Override
  public void setIntHeader(String name, int value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      contentLength = value;
    }
    super.setIntHeader(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    // checking content-type is just an optimization to avoid unnecessary parsing
    if ("Content-Length".equalsIgnoreCase(name) && isContentTypeTextHtml()) {
      contentLength = value;
    }
    super.addIntHeader(name, value);
  }

  @Override
  public void setContentLength(int len) {
    contentLength = len;
    super.setContentLength(len);
  }

  @Nullable
  private static MethodHandle findSetContentLengthLongMethod() {
    try {
      return MethodHandles.lookup()
          .findSpecial(
              HttpServletResponseWrapper.class,
              "setContentLengthLong",
              MethodType.methodType(void.class),
              SnippetInjectingResponseWrapper.class);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      return null;
    }
  }

  // this is for Servlet 3.1 support
  public void setContentLengthLong(long length) throws Throwable {
    contentLength = length;
    if (setContentLengthLongHandler == null) {
      super.setContentLength((int) length);
    } else {
      setContentLengthLongHandler.invokeWithArguments(this, length);
    }
  }

  @Override
  public boolean isContentTypeTextHtml() {
    String contentType = super.getContentType();
    if (contentType == null) {
      contentType = super.getHeader("content-type");
    }
    return contentType != null
        && (contentType.startsWith("text/html") || contentType.startsWith("application/xhtml+xml"));
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    ServletOutputStream output = super.getOutputStream();
    initializeInjectionStateIfNeeded(output, this);
    return output;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (!isContentTypeTextHtml()) {
      return super.getWriter();
    }
    if (snippetInjectingPrintWriter == null) {
      snippetInjectingPrintWriter =
          new SnippetInjectingPrintWriter(super.getWriter(), snippet, this);
    }
    return snippetInjectingPrintWriter;
  }

  @Override
  public void updateContentLengthIfPreviouslySet() {
    if (contentLength != UNSET) {
      setContentLength((int) contentLength + snippetLength);
    }
  }

  @Override
  public boolean isNotSafeToInject() {
    // if content-length was set and response was already committed (headers sent to the client),
    // then it is not safe to inject because the content-length header cannot be updated to account
    // for the snippet length
    return contentLength != UNSET && isCommitted();
  }
}
