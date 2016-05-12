/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.Value;

/**
 * A factory class for creating values based on {@link ValueMapEditModel}
 */
public final class EditModelValues {

  private EditModelValues() {}

  /**
   * Instantiates a new Value based on the value identified by <code>key</code> in
   * the given value map edit model
   * @param editModel the value map edit model
   * @param key the key of the value
   * @param <V> the value type
   * @return a Value base on the given edit model value
   */
  public static <V> Value<V> value(final ValueMapEditModel editModel, final Object key) {
    return new EditModelValue<>(editModel, key);
  }

  private static final class EditModelValue<V> implements Value<V> {

    private final ValueMapEditModel<Object, V> editModel;
    private final Event<V> changeEvent = Events.event();
    private final Object key;

    /**
     * Instantiates a new EditModelValue
     * @param editModel the edit model
     * @param key the key associated with the value
     */
    private EditModelValue(final ValueMapEditModel editModel, final Object key) {
      this.editModel = editModel;
      this.key = key;
      this.editModel.getValueObserver(key).addInfoListener(new EventInfoListener<ValueChange<Object, ?>>() {
        @Override
        public void eventOccurred(final ValueChange info) {
          changeEvent.fire((V) info.getNewValue());
        }
      });
    }

    @Override
    public V get() {
      return editModel.getValue(key);
    }

    @Override
    public void set(final V value) {
      editModel.setValue(key, value);
    }

    @Override
    public EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }
  }
}
