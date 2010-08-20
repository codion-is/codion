package org.jminor.common.model.valuemap;

import org.jminor.common.model.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A default ValueMap implementation.
 * @param <K> the key type
 * @param <V> the value type
 */
public class ValueMapImpl<K, V> implements ValueMap<K, V> {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values = new HashMap<K, V>();

  private static final int MAGIC_NUMBER = 23;

  public ValueMapImpl() {}

  /** {@inheritDoc} */
  public boolean isValueNull(final K key) {
    return getValue(key) == null;
  }

  /** {@inheritDoc} */
  public V setValue(final K key, final V value) {
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, value);
    handleValueSet(key, value, previousValue, initialization);

    return previousValue;
  }

  /** {@inheritDoc} */
  public V getValue(final K key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  public String getValueAsString(final K key) {
    final V value = values.get(key);
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  /** {@inheritDoc} */
  public ValueMap<K, V> getInstance() {
    return new ValueMapImpl<K, V>();
  }

  /** {@inheritDoc} */
  public V copyValue(final V value) {
    return value;
  }

  /**
   * Two ValueChangeMapImpl objects are equal if they contain the
   * same number of values and all their values are equal.
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof ValueMapImpl)) {
      return false;
    }

    final ValueMapImpl<K, V> otherMap = (ValueMapImpl<K, V>) obj;
    if (size() != otherMap.size()) {
      return false;
    }

    for (final K key : otherMap.getValueKeys()) {
      if (!containsValue(key) || !Util.equal(otherMap.getValue(key), getValue(key))) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = MAGIC_NUMBER;
    for (final Object value : getValues()) {
      hash = hash + (value == null ? 0 : value.hashCode());
    }

    return hash;
  }

  /** {@inheritDoc} */
  public final boolean containsValue(final K key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  public final V removeValue(final K key) {
    if (values.containsKey(key)) {
      final V value = values.remove(key);
      handleValueRemoved(key, value);

      return value;
    }

    return null;
  }

  /** {@inheritDoc} */
  public final Collection<K> getValueKeys() {
    return Collections.unmodifiableCollection(values.keySet());
  }

  /** {@inheritDoc} */
  public final void clear() {
    values.clear();
    handleClear();
  }

  /** {@inheritDoc} */
  public final int size() {
    return values.size();
  }

  /** {@inheritDoc} */
  public final ValueMap<K, V> getCopy() {
    final ValueMap<K, V> copy = getInstance();
    copy.setAs(this);

    return copy;
  }

  /** {@inheritDoc} */
  public final void setAs(final ValueMap<K, V> sourceMap) {
    clear();
    if (sourceMap != null) {
      for (final K entryKey : sourceMap.getValueKeys()) {
        final V value = copyValue(sourceMap.getValue(entryKey));
        setValue(entryKey, value);
      }
    }
    handleSetAs(sourceMap);
  }

  /** {@inheritDoc} */
  public final Collection<V> getValues() {
    return Collections.unmodifiableCollection(values.values());
  }

  /**
   * Called after a value has been set.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   */
  protected void handleValueSet(final K key, final V value, final V previousValue, final boolean initialization) {}

  /**
   * Called after a value has been removed from this map.
   * @param key the key
   * @param value the value that was removed
   */
  protected void handleValueRemoved(final K key, final V value) {}

  /**
   * Called after the value map has been cleared.
   */
  protected void handleClear() {}

  /**
   * Called after the value map has been set.
   * @param sourceMap the source map
   */
  protected void handleSetAs(final ValueMap<K, V> sourceMap) {}
}
