/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.db.Attribute;

import java.util.Objects;

/**
 * Factory class for {@link ValueChange} instances
 */
public final class ValueChanges {

  private ValueChanges() {}

  /**
   * Returns a new {@link ValueChange} instance
   * @param <K> the type of the value key
   * @param <V> the type of the value
   * @param key the key associated with the value
   * @param newValue the new value
   * @param oldValue the old value
   * @param initialization true if the value was being initialized
   * @return a new {@link ValueChange} instance
   */
  public static <K extends Attribute, V> ValueChange<K, V> valueChange(final K key, final V newValue, final V oldValue, final boolean initialization) {
    return new DefaultValueChange<>(key, newValue, oldValue, initialization);
  }

  private static final class DefaultValueChange<K extends Attribute, V> implements ValueChange<K, V> {

    /**
     * The key identifying the value having changed
     */
    private final K key;

    /**
     * The new value
     */
    private final V newValue;

    /**
     * The old value
     */
    private final V oldValue;

    /**
     * True if this value change indicates an initialization, that is, a value was not present before this value change
     */
    private final boolean initialization;

    /**
     * Instantiates a new DefaultValueChange
     * @param source the source of the value change
     * @param key the key associated with the value
     * @param newValue the new value
     * @param oldValue the old value
     * @param initialization true if the value was being initialized, as in, no previous value exists
     */
    private DefaultValueChange(final K key, final V newValue, final V oldValue, final boolean initialization) {
      this.key = Objects.requireNonNull(key, "key");
      this.newValue = newValue;
      this.oldValue = oldValue;
      this.initialization = initialization;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getOldValue() {
      return oldValue;
    }

    @Override
    public V getNewValue() {
      return newValue;
    }

    @Override
    public boolean isInitialization() {
      return initialization;
    }

    @Override
    public String toString() {
      if (initialization) {
        return key + ": " + newValue;
      }
      else {
        return key + ": " + oldValue + " -> " + newValue;
      }
    }
  }
}
