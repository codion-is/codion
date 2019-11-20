/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;

/**
 * A base Value implementation handling everything except the value itself.
 * @param <V> the value type
 */
public abstract class AbstractValue<V> extends AbstractObservableValue<V> {

  private final Event<V> changeEvent = Events.event();

  /** {@inheritDoc} */
  @Override
  public final EventObserver<V> getChangeObserver() {
    return changeEvent.getObserver();
  }

  /**
   * Fires the change event for this value, indicating that the underlying value
   * has changed or at least that it may have changed
   * @param value the new value
   */
  protected final void fireChangeEvent(final V value) {
    changeEvent.fire(value);
  }
}
