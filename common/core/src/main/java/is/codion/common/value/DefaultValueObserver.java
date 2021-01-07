/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import java.util.Objects;
import java.util.Optional;

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
  public Optional<V> toOptional() {
    if (isNullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  @Override
  public boolean isNull() {
    return value.isNull();
  }

  @Override
  public boolean isNotNull() {
    return value.isNotNull();
  }

  @Override
  public boolean isNullable() {
    return value.isNullable();
  }

  @Override
  public boolean is(final V value) {
    return Objects.equals(get(), value);
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
