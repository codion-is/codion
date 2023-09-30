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
  public void addListener(Runnable listener) {
    value.addListener(listener);
  }

  @Override
  public void removeListener(Runnable listener) {
    value.removeListener(listener);
  }

  @Override
  public void addDataListener(Consumer<T> listener) {
    value.addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<T> listener) {
    value.removeDataListener(listener);
  }

  @Override
  public void addWeakListener(Runnable listener) {
    value.addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(Runnable listener) {
    value.removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(Consumer<T> listener) {
    value.addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<T> listener) {
    value.removeWeakDataListener(listener);
  }
}
