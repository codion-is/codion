/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.db.Attribute;

/**
 * Provides values mapped to keys.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueProvider<K extends Attribute, V> {

  /**
   * Retrieves the value mapped to the given key
   * @param key the key
   * @return the value mapped to the given key, null if no such mapping exists
   */
  V get(final K key);
}
