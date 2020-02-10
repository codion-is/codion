/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.state.State;

import static java.util.Objects.requireNonNull;

/**
 * A boolean value based on a State, null values are translated to 'false'
 */
final class StateValue implements Value<Boolean>  {

  private final State state;

  StateValue(final State state) {
    this.state = requireNonNull(state);
  }

  @Override
  public void set(final Boolean value) {
    state.set(value != null && value);
  }

  @Override
  public Boolean get() {
    return state.get();
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public void addListener(final EventListener listener) {
    state.addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    state.removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<Boolean> listener) {
    state.addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    state.removeDataListener(listener);
  }

  @Override
  public void link(final Value<Boolean> linkedValue) {
    link(linkedValue, false);
  }

  @Override
  public void link(final Value<Boolean> linkedValue, final boolean oneWay) {
    new ValueLink<>(this, linkedValue, oneWay);
  }
}
