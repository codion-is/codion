/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

final class DefaultValue<T> extends AbstractValue<T> {

  private T value;

  DefaultValue(T initialValue, T nullValue) {
    super(nullValue, NotifyOnSet.YES);
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
