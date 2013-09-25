/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * Represents a change in a {@link ValueMap} value
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueChange<K, V> {

  /**
   * @return the source of the value change
   */
  Object getSource();

  /**
   * @return the key of the associated with the changed value
   */
  K getKey();

  /**
   * @return the old value
   */
  V getOldValue();

  /**
   * @return the new value
   */
  V getNewValue();

  /**
   * @return true if the new value is null
   */
  boolean isNewValueNull();

  /**
   * Returns true if the new value is equal to the given value, nulls being equal
   * @param value the value
   * @return true if the given value is the new value
   */
  boolean isNewValueEqual(final Object value);

  /**
   * Returns true if the old value is equal to the given value, nulls being equal
   * @param value the value
   * @return true if the given value is the old value
   */
  boolean isOldValueEqual(final Object value);

  /**
   * @return true if the old value is null
   */
  boolean isOldValueNull();

  /**
   * @return true if this key had no associated value prior to this value change
   */
  boolean isInitialization();
}
