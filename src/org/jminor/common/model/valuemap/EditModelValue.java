/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Value;

/**
 * A Value implementation for a ValueMapEditModel
 * @param <K> the type of the value map keys
 * @param <V> they type of the value map values
 */
public final class EditModelValue<K, V> implements Value<V> {

  private final ValueMapEditModel<K, V> editModel;
  private final K key;

  /**
   * Instantiates a new EditModelValue
   * @param editModel the edit model
   * @param key the key associated with the value
   */
  public EditModelValue(final ValueMapEditModel editModel, final K key) {
    this.editModel = editModel;
    this.key = key;
  }

  /** {@inheritDoc} */
  @Override
  public V get() {
    return editModel.getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public void set(final V value) {
    editModel.setValue(key, value);
  }

  /** {@inheritDoc} */
  @Override
  public EventObserver getChangeEvent() {
    return editModel.getValueChangeObserver(key);
  }
}
