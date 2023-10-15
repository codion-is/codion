/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.function.Consumer;

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
  public boolean nullable() {
    return value.nullable();
  }

  @Override
  public boolean addListener(Runnable listener) {
    return value.addListener(listener);
  }

  @Override
  public boolean removeListener(Runnable listener) {
    return value.removeListener(listener);
  }

  @Override
  public boolean addDataListener(Consumer<T> listener) {
    return value.addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<T> listener) {
    return value.removeDataListener(listener);
  }

  @Override
  public boolean addWeakListener(Runnable listener) {
    return value.addWeakListener(listener);
  }

  @Override
  public boolean removeWeakListener(Runnable listener) {
    return value.removeWeakListener(listener);
  }

  @Override
  public boolean addWeakDataListener(Consumer<T> listener) {
    return value.addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<T> listener) {
    return value.removeWeakDataListener(listener);
  }
}
