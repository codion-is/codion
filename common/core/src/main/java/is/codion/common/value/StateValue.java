/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.state.State;

import static java.util.Objects.requireNonNull;

/**
 * A boolean value based on a State, null values are translated to 'false'
 */
final class StateValue extends AbstractValue<Boolean>  {

  private final State state;

  StateValue(final State state) {
    super(false);
    this.state = requireNonNull(state);
    this.state.addListener(this::notifyValueChange);
  }

  @Override
  public Boolean get() {
    return state.get();
  }

  @Override
  protected void setValue(final Boolean value) {
    state.set(Boolean.TRUE.equals(value));
  }
}
