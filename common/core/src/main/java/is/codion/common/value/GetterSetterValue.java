/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class GetterSetterValue<T> extends AbstractValue<T> {

  private final Supplier<T> getter;
  private final Consumer<T> setter;

  GetterSetterValue(Supplier<T> getter, Consumer<T> setter, T nullValue) {
    super(nullValue, true);
    this.getter = requireNonNull(getter);
    this.setter = requireNonNull(setter);
  }

  @Override
  public T get() {
    return getter.get();
  }

  @Override
  protected void setValue(T value) {
    setter.accept(value);
  }
}
