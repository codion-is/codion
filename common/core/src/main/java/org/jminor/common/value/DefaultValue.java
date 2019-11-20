/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import java.util.Objects;

final class DefaultValue<V> extends AbstractValue<V> {

  private final V nullValue;
  private V value;

  DefaultValue(final V initialValue, final V nullValue) {
    this.value = initialValue == null ? nullValue : initialValue;
    this.nullValue = nullValue;
  }

  @Override
  public void set(final V value) {
    final V actualValue = value == null ? nullValue : value;
    if (!Objects.equals(this.value, actualValue)) {
      this.value = actualValue;
      fireChangeEvent(this.value);
    }
  }

  @Override
  public V get() {
    return value;
  }

  @Override
  public boolean isNullable() {
    return nullValue == null;
  }
}
