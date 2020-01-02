/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

/**
 * Represents a change in a {@link ValueMap} value
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueChange<K, V> {

  /**
   * @return the key associated with the changed value
   */
  K getKey();

  /**
   * @return the previous value
   */
  V getPreviousValue();

  /**
   * @return the current value
   */
  V getCurrentValue();

  /**
   * @return true if this key had no associated value prior to this value change
   */
  boolean isInitialization();
}
