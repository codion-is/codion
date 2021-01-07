/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.state.State;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

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
  public Optional<Boolean> toOptional() {
    return Optional.of(state.get());
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isNotNull() {
    return !isNull();
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public boolean is(final Boolean value) {
    return Objects.equals(get(), value);
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
  public void removeDataListener(final EventDataListener<Boolean> listener) {
    state.removeDataListener(listener);
  }

  @Override
  public void link(final Value<Boolean> originalValue) {
    new ValueLink<>(this, originalValue);
  }

  @Override
  public void link(final ValueObserver<Boolean> originalValueObserver) {
    set(requireNonNull(originalValueObserver, "originalValueObserver").get());
    originalValueObserver.addDataListener(this::set);
  }

  @Override
  public void addValidator(final Validator<Boolean> validator) {
    throw new UnsupportedOperationException("Validation not implemented for state values");
  }

  @Override
  public Collection<Validator<Boolean>> getValidators() {
    throw new UnsupportedOperationException("Validation not implemented for state values");
  }
}
