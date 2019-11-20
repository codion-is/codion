/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventObserver;

import static java.util.Objects.requireNonNull;

final class DefaultValueObserver<V> implements ValueObserver<V> {

  private final Value<V> value;

  DefaultValueObserver(final Value<V> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public V get() {
    return value.get();
  }

  @Override
  public boolean isNullable() {
    return value.isNullable();
  }

  @Override
  public EventObserver<V> getChangeObserver() {
    return value.getChangeObserver();
  }
}
