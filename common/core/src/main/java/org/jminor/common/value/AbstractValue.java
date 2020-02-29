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
 * When extending this class remember to always call {@link #notifyValueChange()}
 * when the underlying value changes.
 * @param <V> the value type
 */
public abstract class AbstractValue<V> implements Value<V> {

  private final Event<V> changeEvent = Events.event();

  @Override
  public final void addListener(final EventListener listener) {
    changeEvent.addListener(listener);
  }

  @Override
  public final void removeListener(final EventListener listener) {
    changeEvent.removeListener(listener);
  }

  @Override
  public final void addDataListener(final EventDataListener<V> listener) {
    changeEvent.addDataListener(listener);
  }

  @Override
  public final void removeDataListener(final EventDataListener listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public final void link(final Value<V> linkedValue) {
    new ValueLink<V>(this, linkedValue);
  }

  /**
   * Fires the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.onEvent(get());
  }
}
