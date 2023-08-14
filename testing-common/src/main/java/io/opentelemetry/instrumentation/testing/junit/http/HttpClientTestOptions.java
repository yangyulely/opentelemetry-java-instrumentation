/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.testing.junit.http;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;

@AutoValue
public abstract class HttpClientTestOptions {
  public static final Set<AttributeKey<?>> DEFAULT_HTTP_ATTRIBUTES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  SemanticAttributes.NET_PROTOCOL_NAME,
                  SemanticAttributes.NET_PROTOCOL_VERSION,
                  SemanticAttributes.NET_PEER_NAME,
                  SemanticAttributes.NET_PEER_PORT,
                  SemanticAttributes.HTTP_URL,
                  SemanticAttributes.HTTP_METHOD,
                  SemanticAttributes.USER_AGENT_ORIGINAL)));

  public static final BiFunction<URI, String, String> DEFAULT_EXPECTED_CLIENT_SPAN_NAME_MAPPER =
      (uri, method) -> method;

  public static final int FOUND_STATUS_CODE = HttpStatus.FOUND.code();

  public abstract Function<URI, Set<AttributeKey<?>>> getHttpAttributes();

  @Nullable
  public abstract Integer getResponseCodeOnRedirectError();

  @Nullable
  public abstract String getUserAgent();

  public abstract BiFunction<URI, Throwable, Throwable> getClientSpanErrorMapper();

  /**
   * The returned function should create either a single connection to the target uri or a http
   * client which is guaranteed to use the same connection for all requests.
   */
  public abstract BiFunction<String, Integer, SingleConnection> getSingleConnectionFactory();

  public abstract BiFunction<URI, String, String> getExpectedClientSpanNameMapper();

  abstract HttpClientInstrumentationType getInstrumentationType();

  public boolean isLowLevelInstrumentation() {
    return getInstrumentationType() == HttpClientInstrumentationType.LOW_LEVEL;
  }

  public abstract boolean getTestWithClientParent();

  public abstract boolean getTestRedirects();

  public abstract boolean getTestCircularRedirects();

  /** Returns the maximum number of redirects that http client follows before giving up. */
  public abstract int getMaxRedirects();

  public abstract boolean getTestReusedRequest();

  public abstract boolean getTestConnectionFailure();

  public abstract boolean getTestReadTimeout();

  public abstract boolean getTestRemoteConnection();

  public abstract boolean getTestHttps();

  public abstract boolean getTestCallback();

  public abstract boolean getTestCallbackWithParent();

  // depending on async behavior callback can be executed within
  // parent span scope or outside of the scope, e.g. in reactor-netty or spring
  // callback is correlated.
  public abstract boolean getTestCallbackWithImplicitParent();

  public abstract boolean getTestErrorWithCallback();

  static Builder builder() {
    return new AutoValue_HttpClientTestOptions.Builder().withDefaults();
  }

  @AutoValue.Builder
  public interface Builder {

    @CanIgnoreReturnValue
    default Builder withDefaults() {
      return setHttpAttributes(x -> DEFAULT_HTTP_ATTRIBUTES)
          .setResponseCodeOnRedirectError(FOUND_STATUS_CODE)
          .setUserAgent(null)
          .setClientSpanErrorMapper((uri, exception) -> exception)
          .setSingleConnectionFactory((host, port) -> null)
          .setExpectedClientSpanNameMapper(DEFAULT_EXPECTED_CLIENT_SPAN_NAME_MAPPER)
          .setInstrumentationType(HttpClientInstrumentationType.HIGH_LEVEL)
          .setTestWithClientParent(true)
          .setTestRedirects(true)
          .setTestCircularRedirects(true)
          .setMaxRedirects(2)
          .setTestReusedRequest(true)
          .setTestConnectionFailure(true)
          .setTestReadTimeout(true)
          .setTestRemoteConnection(true)
          .setTestHttps(true)
          .setTestCallback(true)
          .setTestCallbackWithParent(true)
          .setTestCallbackWithImplicitParent(false)
          .setTestErrorWithCallback(true);
    }

    Builder setHttpAttributes(Function<URI, Set<AttributeKey<?>>> value);

    Builder setResponseCodeOnRedirectError(Integer value);

    Builder setUserAgent(String value);

    Builder setClientSpanErrorMapper(BiFunction<URI, Throwable, Throwable> value);

    Builder setSingleConnectionFactory(BiFunction<String, Integer, SingleConnection> value);

    Builder setExpectedClientSpanNameMapper(BiFunction<URI, String, String> value);

    Builder setInstrumentationType(HttpClientInstrumentationType instrumentationType);

    Builder setTestWithClientParent(boolean value);

    Builder setTestRedirects(boolean value);

    Builder setTestCircularRedirects(boolean value);

    Builder setMaxRedirects(int value);

    Builder setTestReusedRequest(boolean value);

    Builder setTestConnectionFailure(boolean value);

    Builder setTestReadTimeout(boolean value);

    Builder setTestRemoteConnection(boolean value);

    Builder setTestHttps(boolean value);

    Builder setTestCallback(boolean value);

    Builder setTestCallbackWithParent(boolean value);

    Builder setTestCallbackWithImplicitParent(boolean value);

    Builder setTestErrorWithCallback(boolean value);

    @CanIgnoreReturnValue
    default Builder disableTestWithClientParent() {
      return setTestWithClientParent(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestRedirects() {
      return setTestRedirects(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestCircularRedirects() {
      return setTestCircularRedirects(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestReusedRequest() {
      return setTestReusedRequest(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestConnectionFailure() {
      return setTestConnectionFailure(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestReadTimeout() {
      return setTestReadTimeout(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestRemoteConnection() {
      return setTestRemoteConnection(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestHttps() {
      return setTestHttps(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestCallback() {
      return setTestCallback(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestCallbackWithParent() {
      return setTestCallbackWithParent(false);
    }

    @CanIgnoreReturnValue
    default Builder disableTestErrorWithCallback() {
      return setTestErrorWithCallback(false);
    }

    @CanIgnoreReturnValue
    default Builder enableTestCallbackWithImplicitParent() {
      return setTestCallbackWithImplicitParent(true);
    }

    @CanIgnoreReturnValue
    default Builder markAsLowLevelInstrumentation() {
      return setInstrumentationType(HttpClientInstrumentationType.LOW_LEVEL);
    }

    HttpClientTestOptions build();
  }

  enum HttpClientInstrumentationType {
    /**
     * Creates a span for each attempt to send an HTTP request over the wire, follows the HTTP
     * resend spec.
     */
    LOW_LEVEL,
    /** Creates a single span for the topmost HTTP client operation. */
    HIGH_LEVEL
  }
}
