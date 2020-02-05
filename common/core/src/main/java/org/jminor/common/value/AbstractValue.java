/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;

/**
 * A base Value implementation handling everything except the value itself.
 * When extending this class remember to always call {@link #notifyValueChange(Object)}
 * when the underlying value changes.
 * @param <V> the value type
 */
public abstract class AbstractValue<V> implements Value<V> {

  private final Event<V> changeEvent = Events.event();

  @Override
  public void addListener(final EventListener listener) {
    changeEvent.addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    changeEvent.removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<V> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    changeEvent.removeDataListener(listener);
  }

  /**
   * Fires the change event for this value, indicating that the underlying value
   * has changed or at least that it may have changed
   * @param value the new value
   */
  protected final void notifyValueChange(final V value) {
    changeEvent.onEvent(value);
  }
}
