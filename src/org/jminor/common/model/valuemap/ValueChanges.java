/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Util;

/**
 * Factory class for {@link ValueChange} instances
 */
public final class ValueChanges {

  private ValueChanges() {}

  /**
   * Returns a new {@link ValueChange} instance
   * @param source the source of the value change
   * @param key the key associated with the value
   * @param newValue the new value
   * @param oldValue the old value
   * @param initialization true if the value was being initialized
   */
  public static <K, V> ValueChange<K, V> valueChange(final Object source, final K key, final V newValue, final V oldValue,
                                                     final boolean initialization) {
    return new DefaultValueChange<>(source, key, newValue, oldValue, initialization);
  }

  private static final class DefaultValueChange<K, V> implements ValueChange<K, V> {

    /**
     * The source of the value change
     */
    private final Object source;

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
    public DefaultValueChange(final Object source, final K key, final V newValue, final V oldValue, final boolean initialization) {
      Util.rejectNullValue(key, "key");
      this.source = source;
      this.key = key;
      this.newValue = newValue;
      this.oldValue = oldValue;
      this.initialization = initialization;
    }

    /**
     * @return the source of the value change
     */
    @Override
    public Object getSource() {
      return source;
    }

    /**
     * @return the key of the associated with the changed value
     */
    @Override
    public K getKey() {
      return key;
    }

    /**
     * @return the old value
     */
    @Override
    public V getOldValue() {
      return oldValue;
    }

    /**
     * @return the new value
     */
    @Override
    public V getNewValue() {
      return newValue;
    }

    /**
     * @return true if the new value is null
     */
    @Override
    public boolean isNewValueNull() {
      return newValue == null;
    }

    /**
     * Returns true if the new value is equal to the given value, nulls being equal
     * @param value the value
     * @return true if the given value is the new value
     */
    @Override
    public boolean isNewValueEqual(final Object value) {
      return Util.equal(newValue, value);
    }

    /**
     * Returns true if the old value is equal to the given value, nulls being equal
     * @param value the value
     * @return true if the given value is the old value
     */
    @Override
    public boolean isOldValueEqual(final Object value) {
      return Util.equal(oldValue, value);
    }

    /**
     * @return true if the old value is null
     */
    @Override
    public boolean isOldValueNull() {
      return oldValue == null;
    }

    /**
     * @return true if this key had no associated value prior to this value change
     */
    @Override
    public boolean isInitialization() {
      return initialization;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      if (initialization) {
        return ValueChange.class.getName() + ", " + key + ": " + newValue;
      }
      else {
        return ValueChange.class.getName() + ", " + key + ": " + oldValue + " -> " + newValue;
      }
    }
  }
}
