/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

final class DefaultValue<T> extends AbstractValue<T> {

  private T value;

  DefaultValue(T initialValue, T nullValue, Notify notify) {
    super(nullValue, notify);
    set(initialValue);
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  protected void setValue(T value) {
    this.value = value;
  }
}
