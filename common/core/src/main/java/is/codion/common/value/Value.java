/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

/**
 * A wrapper class for setting and getting a value
 * @param <V> the type of the value
 */
public interface Value<V> extends ValueObserver<V> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(V value);

  /**
   * Creates a bidirectional link between this and the given value,
   * so that changes in one are reflected in the other.
   * Note that after a call to this method the value of {@code linkedValue} is the same as this value.
   * @param linkedValue the value to link
   */
  void link(Value<V> linkedValue);
}
