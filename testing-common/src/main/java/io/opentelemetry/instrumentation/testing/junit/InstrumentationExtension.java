/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.testing.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.instrumentation.testing.InstrumentationTestRunner;
import io.opentelemetry.instrumentation.testing.LibraryTestRunner;
import io.opentelemetry.instrumentation.testing.util.ContextStorageCloser;
import io.opentelemetry.instrumentation.testing.util.ThrowingRunnable;
import io.opentelemetry.instrumentation.testing.util.ThrowingSupplier;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.assertj.MetricAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class InstrumentationExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

  private final InstrumentationTestRunner testRunner;

  protected InstrumentationExtension(InstrumentationTestRunner testRunner) {
    this.testRunner = testRunner;
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    testRunner.beforeTestClass();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    testRunner.clearAllExportedData();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    ContextStorage storage = ContextStorage.get();
    ContextStorageCloser.close(storage);
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    testRunner.afterTestClass();
  }

  /** Return the {@link OpenTelemetry} instance used to produce telemetry data. */
  public OpenTelemetry getOpenTelemetry() {
    return testRunner.getOpenTelemetry();
  }

  /** Return a list of all captured spans. */
  public List<SpanData> spans() {
    return testRunner.getExportedSpans();
  }

  /** Return a list of all captured metrics. */
  public List<MetricData> metrics() {
    return testRunner.getExportedMetrics();
  }

  private List<MetricData> instrumentationMetrics(String instrumentationName) {
    return metrics().stream()
        .filter(m -> m.getInstrumentationScopeInfo().getName().equals(instrumentationName))
        .collect(Collectors.toList());
  }

  /** Return a list of all captured logs. */
  public List<LogRecordData> logRecords() {
    return testRunner.getExportedLogRecords();
  }

  /**
   * Waits for the assertion applied to all metrics of the given instrumentation and metric name to
   * pass.
   */
  public void waitAndAssertMetrics(
      String instrumentationName, String metricName, Consumer<ListAssert<MetricData>> assertion) {
    await()
        .untilAsserted(
            () ->
                assertion.accept(
                    assertThat(metrics())
                        .filteredOn(
                            data ->
                                data.getInstrumentationScopeInfo()
                                        .getName()
                                        .equals(instrumentationName)
                                    && data.getName().equals(metricName))));
  }

  @SafeVarargs
  public final void waitAndAssertMetrics(
      String instrumentationName, Consumer<MetricAssert>... assertions) {
    await()
        .untilAsserted(
            () -> {
              Collection<MetricData> metrics = instrumentationMetrics(instrumentationName);
              assertThat(metrics).isNotEmpty();
              for (Consumer<MetricAssert> assertion : assertions) {
                assertThat(metrics)
                    .anySatisfy(
                        metric -> assertion.accept(OpenTelemetryAssertions.assertThat(metric)));
              }
            });
  }

  /**
   * Removes all captured telemetry data. After calling this method {@link #spans()} and {@link
   * #metrics()} will return empty lists until more telemetry data is captured.
   */
  public void clearData() {
    testRunner.clearAllExportedData();
  }

  /**
   * Wait until at least {@code numberOfTraces} traces are completed and return all captured traces.
   * Note that there may be more than {@code numberOfTraces} collected. This waits up to 20 seconds,
   * then times out.
   */
  public List<List<SpanData>> waitForTraces(int numberOfTraces) {
    return testRunner.waitForTraces(numberOfTraces);
  }

  /**
   * Wait until at least {@code numberOfLogRecords} log records are completed and return all
   * captured log records. Note that there may be more than {@code numberOfLogRecords} collected.
   * This waits up to 20 seconds, then times out.
   */
  public List<LogRecordData> waitForLogRecords(int numberOfLogRecords) {
    await()
        .timeout(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertThat(testRunner.getExportedLogRecords().size())
                    .isEqualTo(numberOfLogRecords));
    return testRunner.getExportedLogRecords();
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertSortedTraces(
      Comparator<List<SpanData>> traceComparator, Consumer<TraceAssert>... assertions) {
    testRunner.waitAndAssertSortedTraces(traceComparator, assertions);
  }

  public final void waitAndAssertSortedTraces(
      Comparator<List<SpanData>> traceComparator,
      Iterable<? extends Consumer<TraceAssert>> assertions) {
    testRunner.waitAndAssertSortedTraces(traceComparator, assertions);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertTracesWithoutScopeVersionVerification(
      Consumer<TraceAssert>... assertions) {
    testRunner.waitAndAssertTracesWithoutScopeVersionVerification(assertions);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertTraces(Consumer<TraceAssert>... assertions) {
    testRunner.waitAndAssertTraces(assertions);
  }

  public final void waitAndAssertTraces(Iterable<? extends Consumer<TraceAssert>> assertions) {
    testRunner.waitAndAssertTraces(assertions);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an INTERNAL span with name {@code
   * spanName}.
   */
  public <E extends Exception> void runWithSpan(String spanName, ThrowingRunnable<E> callback)
      throws E {
    testRunner.runWithSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an INTERNAL span with name {@code
   * spanName}.
   */
  public <T, E extends Throwable> T runWithSpan(String spanName, ThrowingSupplier<T, E> callback)
      throws E {
    return testRunner.runWithSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP CLIENT span with name {@code
   * spanName}.
   */
  public <E extends Throwable> void runWithHttpClientSpan(
      String spanName, ThrowingRunnable<E> callback) throws E {
    testRunner.runWithHttpClientSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP CLIENT span with name {@code
   * spanName}.
   */
  public <T, E extends Throwable> T runWithHttpClientSpan(
      String spanName, ThrowingSupplier<T, E> callback) throws E {
    return testRunner.runWithHttpClientSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an CLIENT span with name {@code
   * spanName}.
   */
  public <E extends Throwable> void runWithHttpServerSpan(ThrowingRunnable<E> callback) throws E {
    testRunner.runWithHttpServerSpan(callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an CLIENT span with name {@code
   * spanName}.
   */
  public <T, E extends Throwable> T runWithHttpServerSpan(ThrowingSupplier<T, E> callback)
      throws E {
    return testRunner.runWithHttpServerSpan(callback);
  }

  /** Returns whether forceFlush was called. */
  public boolean forceFlushCalled() {
    return testRunner.forceFlushCalled();
  }

  /** Returns the {@link OpenTelemetrySdk} initialized for library tests. */
  public OpenTelemetrySdk getOpenTelemetrySdk() {
    if (testRunner instanceof LibraryTestRunner) {
      return ((LibraryTestRunner) testRunner).getOpenTelemetrySdk();
    }
    throw new IllegalStateException("Can only be called from library instrumentation tests.");
  }

  protected InstrumentationTestRunner getTestRunner() {
    return testRunner;
  }
}
