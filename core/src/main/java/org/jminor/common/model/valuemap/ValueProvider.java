/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * Provides values mapped to keys.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueProvider<K, V> {

  /**
   * Retrieves the value mapped to the given key
   * @param key the key
   * @return the value mapped to the given key, null if no such mapping exists
   */
  V getValue(final K key);
}
