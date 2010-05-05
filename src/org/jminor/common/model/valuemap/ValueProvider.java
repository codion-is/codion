/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * Provides values mapped to keys.<br>
 * User: Bjorn Darri<br>
 * Date: 4.4.2010<br>
 * Time: 21:06:22<br>
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueProvider<K, V> {

  /**
   * Retrieves the value mapped to the given key
   * @param key the key
   * @return the value mapped to the given key, null if no such mapping exists
   */
  public V getValue(final K key);
}
