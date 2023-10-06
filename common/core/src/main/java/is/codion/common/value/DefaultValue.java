/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

final class DefaultValue<T> extends AbstractValue<T> {

  private T value;

  DefaultValue(T initialValue, T nullValue) {
    super(nullValue, Notify.WHEN_CHANGED);
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
