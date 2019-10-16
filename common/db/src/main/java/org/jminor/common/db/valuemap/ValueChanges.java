/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.db.Attribute;

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
   * @param initialization true if the value was being initialized
   * @return a new {@link ValueChange} instance
   */
  public static <K extends Attribute, V> ValueChange<K, V> valueChange(final K key, final V currentValue, final V previousValue, final boolean initialization) {
    return new DefaultValueChange<>(key, currentValue, previousValue, initialization);
  }

  private static final class DefaultValueChange<K extends Attribute, V> implements ValueChange<K, V> {

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
     * True if this value change indicates an initialization, that is, a value was not present before this value change
     */
    private final boolean initialization;

    /**
     * Instantiates a new DefaultValueChange
     * @param source the source of the value change
     * @param key the key associated with the value
     * @param currentValue the current value
     * @param previousValue the previous value
     * @param initialization true if the value was being initialized, as in, no previous value existed
     */
    private DefaultValueChange(final K key, final V currentValue, final V previousValue, final boolean initialization) {
      this.key = requireNonNull(key, "key");
      this.currentValue = currentValue;
      this.previousValue = previousValue;
      this.initialization = initialization;
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
    public boolean isInitialization() {
      return initialization;
    }

    @Override
    public String toString() {
      if (initialization) {
        return key + ": " + currentValue;
      }
      else {
        return key + ": " + previousValue + " -> " + currentValue;
      }
    }
  }
}
