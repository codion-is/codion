/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An interface describing an object containing values mapped to keys
 */
public interface ValueMap {

  /**
   * Retrieves the value mapped to the given key
   * @param key the key
   * @return the value mapped to the given key, null if no such mapping exists
   */
  public Object getValue(final String key);

  /**
   * Maps the given value to the given key, returning the old value if any
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key, null if no such value existed
   */
  public Object setValue(final String key, final Object value);

  /**
   * Returns true if the value mapped to the given key is null
   * @param key the key
   * @return true if the value mapped to the given key is null
   */
  public boolean isValueNull(final String key);

  /**
   * Returns true if this ValueMap contains a value for the given key
   * @param key the key
   * @return true if a value is mapped to this key
   */
  public boolean containsValue(final String key);

  /**
   * Describes an object responsible for providing String representations of ValueMap instances
   */
  public interface ToString {
    public String toString(final ValueMap valueMap);
  }
}
