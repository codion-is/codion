/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import static java.util.Objects.requireNonNull;

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
   * @param currentValue the current value
   * @param previousValue the previous value
   * @return a new {@link ValueChange} instance
   */
  public static <K, V> ValueChange<K, V> valueChange(final K key, final V currentValue, final V previousValue) {
    return new DefaultValueChange<>(key, currentValue, previousValue);
  }

  private static final class DefaultValueChange<K, V> implements ValueChange<K, V> {

    /**
     * The key identifying the changed value
     */
    private final K key;

    /**
     * The new value
     */
    private final V currentValue;

    /**
     * The old value
     */
    private final V previousValue;

    /**
     * Instantiates a new DefaultValueChange
     * @param source the source of the value change
     * @param key the key associated with the value
     * @param currentValue the current value
     * @param previousValue the previous value
     */
    private DefaultValueChange(final K key, final V currentValue, final V previousValue) {
      this.key = requireNonNull(key, "key");
      this.currentValue = currentValue;
      this.previousValue = previousValue;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getPreviousValue() {
      return previousValue;
    }

    @Override
    public V getCurrentValue() {
      return currentValue;
    }

    @Override
    public String toString() {
      return key + ": " + previousValue + " -> " + currentValue;
    }
  }
}
