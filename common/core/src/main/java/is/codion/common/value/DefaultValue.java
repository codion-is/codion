/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

final class DefaultValue<V> extends AbstractValue<V> {

  private V value;

  DefaultValue(final V initialValue, final V nullValue) {
    super(nullValue, NotifyOnSet.YES);
    set(initialValue);
  }

  @Override
  public V get() {
    return value;
  }

  @Override
  protected void setValue(final V value) {
    this.value = value;
  }
}
