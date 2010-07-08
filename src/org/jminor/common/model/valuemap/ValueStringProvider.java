/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * Provides values mapped to keys as strings.<br>
 * User: Bjorn Darri<br>
 * Date: 4.4.2010<br>
 * Time: 21:06:22<br>
 * @param <K> the type of the map keys
 */
public interface ValueStringProvider<K> {

  /**
   * Retrieves the value mapped to the given key as a String
   * @param key the key
   * @return the value mapped to the given key as a string, "null" if no such mapping exists
   */
  String getValueAsString(final K key);
}