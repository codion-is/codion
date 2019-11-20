/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.EventObserver;
import org.jminor.common.state.State;
import org.jminor.common.state.States;

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
   * Instantiates a new Value based on a class property
   * @param owner the property owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeEvent an event which fires each time the value changes
   * @param <V> type to wrap
   * @return a Value for the given property
   */
  public static <V> Value<V> propertyValue(final Object owner, final String propertyName, final Class<V> valueClass,
                                           final EventObserver<V> valueChangeEvent) {
    return new DefaultPropertyValue<>(owner, propertyName, valueClass, valueChangeEvent);
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
    link(booleanValue, stateValue(state));

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

  /**
   * Links the two values together so that changes in one are reflected in the other
   * @param originalValue the original value
   * @param linkedValue the linked value
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> originalValue, final Value<V> linkedValue) {
    link(originalValue, linkedValue, false);
  }

  /**
   * Links the two values together so that changes in one are reflected in the other
   * @param originalValue the original value
   * @param linkedValue the linked value
   * @param oneWay if true the original value is not updated if the linked value changes
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> originalValue, final Value<V> linkedValue, final boolean oneWay) {
    new ValueLink<>(originalValue, linkedValue, oneWay);
  }
}
