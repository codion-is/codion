/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventObserver;
import org.jminor.common.state.State;

import static java.util.Objects.requireNonNull;

/**
 * A boolean value based on a State, null values are translated to 'false'
 */
final class StateValue extends AbstractObservableValue<Boolean> {

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
  public EventObserver<Boolean> getChangeObserver() {
    return state.getObserver();
  }
}
