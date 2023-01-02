/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import static java.util.Objects.requireNonNull;

final class DefaultValueObserver<T> implements ValueObserver<T> {

  private final Value<T> value;

  DefaultValueObserver(Value<T> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public T get() {
    return value.get();
  }

  @Override
  public boolean isNullable() {
    return value.isNullable();
  }

  @Override
  public void addListener(EventListener listener) {
    value.addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    value.removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<T> listener) {
    value.addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<T> listener) {
    value.removeDataListener(listener);
  }
}
