/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * Provides values mapped to keys as strings.
 * @param <K> the type of the map keys
 */
public interface ValueAsStringProvider<K> {

  /**
   * Retrieves the value mapped to the given key as a String
   * @param key the key
   * @return the value mapped to the given key as a string, an empty string if no such mapping exists
   */
  String getValueAsString(final K key);
}