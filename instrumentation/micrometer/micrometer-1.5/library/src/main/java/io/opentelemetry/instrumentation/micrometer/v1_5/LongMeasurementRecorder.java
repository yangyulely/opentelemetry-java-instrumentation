/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.micrometer.v1_5;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import javax.annotation.Nullable;

final class LongMeasurementRecorder<T> implements Consumer<ObservableLongMeasurement> {

  // using a weak reference here so that the existence of the micrometer Meter does not block the
  // measured object from being GC'd; e.g. a Gauge (or any other async instrument) must not block
  // garbage collection of the object that it measures
  private final WeakReference<T> objWeakRef;
  private final ToLongFunction<T> metricFunction;
  private final Attributes attributes;
  private final WeakReference<ClassLoader> contextClassLoader;

  LongMeasurementRecorder(
      @Nullable T obj, ToLongFunction<T> metricFunction, Attributes attributes) {
    this.objWeakRef = new WeakReference<>(obj);
    this.metricFunction = metricFunction;
    this.attributes = attributes;
    contextClassLoader = new WeakReference<>(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public void accept(ObservableLongMeasurement measurement) {
    T obj = objWeakRef.get();
    if (obj != null) {
      MeasurementRecorderUtil.runInThreadContextClassLoader(
          contextClassLoader.get(),
          () -> measurement.record(metricFunction.applyAsLong(obj), attributes));
    }
  }
}
