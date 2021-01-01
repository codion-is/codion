/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.state.States;

import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link Value} objects
 */
public final class Values {

  private Values() {}

  /**
   * Instantiates a new Value instance wrapping a null initial value
   * @param <V> type to wrap
   * @return a Value for the given type
   */
  public static <V> Value<V> value() {
    return value(null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  public static <V> Value<V> value(final V initialValue) {
    return value(initialValue, null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  public static <V> Value<V> value(final V initialValue, final V nullValue) {
    return new DefaultValue<>(initialValue, nullValue);
  }

  /**
   * Instantiates a new empty ValueSet
   * @param <V> the value type
   * @return a ValueSet
   */
  public static <V> ValueSet<V> valueSet() {
    return valueSet(Collections.emptySet());
  }

  /**
   * Instantiates a new ValueSet
   * @param initialValues the initial values, may not be null
   * @param <V> the value type
   * @return a ValueSet
   */
  public static <V> ValueSet<V> valueSet(final Set<V> initialValues) {
    return new DefaultValueSet<>(initialValues);
  }

  /**
   * Instantiates a new PropertyValue based on a class property
   * @param owner the property owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeObserver an observer notified each time the value changes
   * @param <V> type to wrap
   * @return a Value for the given property
   */
  public static <V> PropertyValue<V> propertyValue(final Object owner, final String propertyName, final Class<V> valueClass,
                                                   final EventObserver<V> valueChangeObserver) {
    return new DefaultPropertyValue<>(owner, propertyName, valueClass, valueChangeObserver);
  }

  /**
   * Instantiates a boolean Value based on a {@link State}.
   * Null values are translated to 'false'.
   * @param state the state to base the value on
   * @return a boolean state based on the given value
   */
  public static Value<Boolean> stateValue(final State state) {
    return new StateValue(state);
  }

  /**
   * Instantiates a State linked to the given boolean value.
   * Null values are translated to 'false'.
   * @param booleanValue the boolean value to link to the state
   * @return a State linked to the given value
   */
  public static State valueState(final Value<Boolean> booleanValue) {
    final State state = States.state();
    stateValue(state).link(requireNonNull(booleanValue, "booleanValue"));

    return state;
  }

  /**
   * Instantiates a new ValueObserver for the given value.
   * @param value the value to observe
   * @param <V> the value type
   * @return a ValueObserver for the given value
   */
  public static <V> ValueObserver<V> valueObserver(final Value<V> value) {
    return new DefaultValueObserver<>(value);
  }
}
