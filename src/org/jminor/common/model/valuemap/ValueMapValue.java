/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Value;

public class ValueMapValue<K, V> implements Value<V> {

  private final ValueMapEditModel<K, V> editModel;
  private final K key;

  public ValueMapValue(final ValueMapEditModel<K, V> editModel, final K key) {
    this.editModel = editModel;
    this.key = key;
  }

  /** {@inheritDoc} */
  @Override
  public final V get() {
    return editModel.getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void set(final V value) {
    editModel.setValue(key, value);
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getChangeEvent() {
    return editModel.getValueChangeObserver(key);
  }
}
