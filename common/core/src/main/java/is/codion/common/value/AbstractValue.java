/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;

import java.util.Optional;

/**
 * A base Value implementation handling everything except the value itself.
 * When extending this class remember to always call {@link #notifyValueChange()}
 * when the underlying value changes.
 * @param <V> the value type
 */
public abstract class AbstractValue<V> implements Value<V> {

  private final Event<V> changeEvent = Events.event();

  @Override
  public final Optional<V> toOptional() {
    if (isNullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  @Override
  public final boolean isNull() {
    return get() == null;
  }

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
  public final void removeDataListener(final EventDataListener<V> listener) {
    changeEvent.removeDataListener(listener);
  }

  @Override
  public final void link(final Value<V> originalValue) {
    new ValueLink<>(this, originalValue);
  }

  /**
   * Fires the change event for this value, using the current value, indicating that
   * the underlying value has changed or at least that it may have changed
   */
  protected final void notifyValueChange() {
    changeEvent.onEvent(get());
  }
}
