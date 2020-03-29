/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.valuemap;

import org.jminor.common.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default ValueMap implementation.
 * Note that this class is not thread safe.
 * @param <K> the key type
 * @param <V> the value type
 */
public class DefaultValueMap<K, V> implements ValueMap<K, V> {

  private static final String KEY = "key";

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values;

  /**
   * Holds the original value for keys which values have changed since they were first set.
   */
  private Map<K, V> originalValues;

  /**
   * Instantiates a new empty instance
   */
  public DefaultValueMap() {
    this(null, null);
  }

  /**
   * Instantiates a new instance with the given values and original values.
   * Note that no validation is performed.
   * @param values the values
   * @param originalValues the originalValues
   */
  protected DefaultValueMap(final Map<K, V> values, final Map<K, V> originalValues) {
    this.values = values == null ? new HashMap<>() : new HashMap<>(values);
    this.originalValues = originalValues == null ? null : new HashMap<>(originalValues);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNull(final K key) {
    return get(key) == null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNotNull(final K key) {
    return !isNull(key);
  }

  /** {@inheritDoc} */
  @Override
  public final V put(final K key, final V value) {
    requireNonNull(key, KEY);
    final V newValue = validateAndPrepareForPut(key, value);
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(key, newValue, previousValue);
    }
    onValuePut(key, newValue, previousValue);
    onValueChanged(key, newValue, previousValue, initialization);

    return previousValue;
  }

  /** {@inheritDoc} */
  @Override
  public V get(final K key) {
    return values.get(requireNonNull(key, KEY));
  }

  /**
   * Two DefaultValueMap objects are equal if their current values represent the same mappings.
   * @see Map#equals(Object)
   */
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DefaultValueMap)) {
      return false;
    }

    final DefaultValueMap<K, V> otherMap = (DefaultValueMap<K, V>) obj;

    return otherMap.values.equals(this.values);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return values.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsKey(final K key) {
    return values.containsKey(requireNonNull(key, KEY));
  }

  /** {@inheritDoc} */
  @Override
  public final V remove(final K key) {
    if (values.containsKey(requireNonNull(key, KEY))) {
      final V value = values.remove(key);
      removeOriginalValue(key);
      onValueChanged(key, null, value, false);

      return value;
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final int size() {
    return values.size();
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> keySet() {
    return unmodifiableSet(values.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> originalKeySet() {
    if (originalValues == null) {
      return emptySet();
    }

    return unmodifiableSet(originalValues.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<V> values() {
    return unmodifiableCollection(values.values());
  }

  /** {@inheritDoc} */
  @Override
  public final V getOriginal(final K key) {
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return get(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified() {
    return !Util.nullOrEmpty(originalValues);
  }

  /** {@inheritDoc} */
  @Override
  public final void setAs(final ValueMap<K, V> sourceMap) {
    if (sourceMap == this) {
      return;
    }
    final Set<K> affectedKeys = new HashSet<>(keySet());
    clear();
    if (sourceMap != null) {
      final Collection<K> sourceValueKeys = sourceMap.keySet();
      affectedKeys.addAll(sourceValueKeys);
      for (final K key : sourceValueKeys) {
        values.put(key, sourceMap.get(key));
      }
      if (sourceMap.isModified()) {
        originalValues = new HashMap<>();
        for (final K key : sourceMap.originalKeySet()) {
          originalValues.put(key, sourceMap.getOriginal(key));
        }
      }
    }
    for (final K key : affectedKeys) {
      onValueChanged(key, values.get(key), null, true);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified(final K key) {
    requireNonNull(key, KEY);
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void revert(final K key) {
    if (isModified(key)) {
      put(key, getOriginal(key));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void revertAll() {
    for (final K key : keySet()) {
      revert(key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void save(final K key) {
    requireNonNull(key, KEY);
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void saveAll() {
    originalValues = null;
  }

  /**
   * Silently removes all values from this map, as in, removes the values without firing value change events.
   * super.clear() must be called when overriding.
   */
  protected void clear() {
    values.clear();
    if (originalValues != null) {
      originalValues = null;
    }
  }

  /**
   * Validates the value for the given key, during a {@link #put(Object, Object)} operation,
   * note that this default implementation does nothing, provided for subclasses.
   * @param key the key
   * @param value the value to validate
   * @return the value
   * @throws IllegalArgumentException in case the value is invalid
   */
  protected V validateAndPrepareForPut(final K key, final V value) {
    return value;
  }

  /**
   * Called after a value has been put. This base implementation does nothing, provided for subclasses.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   */
  protected void onValuePut(final K key, final V value, final V previousValue) {/*Provided for subclasses*/}

  /**
   * Called when a value has changed, note that this default implementation does nothing, provided for subclasses.
   * @param key the key of the value that was changed
   * @param currentValue the new value
   * @param previousValue the previous value, if any
   * @param initialization true if the value was being initialized, that is, no previous value existed
   */
  protected void onValueChanged(final K key, final V currentValue, final V previousValue,
                                final boolean initialization) {/*Provided for subclasses*/}

  protected final void setOriginalValue(final K key, final V previousValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(key, previousValue);
  }

  protected final void removeOriginalValue(final K key) {
    if (originalValues != null) {
      originalValues.remove(key);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  private void updateOriginalValue(final K key, final V value, final V previousValue) {
    final boolean modified = isModified(key);
    if (modified && Objects.equals(getOriginal(key), value)) {
      removeOriginalValue(key);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(key, previousValue);
    }
  }
}
