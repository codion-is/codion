/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.value;

import dev.codion.common.event.EventDataListener;
import dev.codion.common.event.EventListener;

import static java.util.Objects.requireNonNull;

final class DefaultValueObserver<V> implements ValueObserver<V> {

  private final Value<V> value;

  DefaultValueObserver(final Value<V> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public V get() {
    return value.get();
  }

  @Override
  public boolean isNullable() {
    return value.isNullable();
  }

  @Override
  public void addListener(final EventListener listener) {
    value.addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    value.removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<V> listener) {
    value.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener<V> listener) {
    value.removeDataListener(listener);
  }
}
