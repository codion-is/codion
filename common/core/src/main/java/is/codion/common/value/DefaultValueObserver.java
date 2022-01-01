/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultValueObserver<T> implements ValueObserver<T> {

  private final Value<T> value;

  DefaultValueObserver(final Value<T> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public T get() {
    return value.get();
  }

  @Override
  public Optional<T> toOptional() {
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
  public boolean equalTo(final T value) {
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
  public void addDataListener(final EventDataListener<T> listener) {
    value.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener<T> listener) {
    value.removeDataListener(listener);
  }
}
