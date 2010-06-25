package org.jminor.common.model.valuemap;

import org.jminor.common.model.Util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValueMapImpl<K, V> implements ValueMap<K, V>, Serializable {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values;

  private static final int DEFAULT_SIZE = 10;

  /**
   * Instantiate a new ValueMap with a default size of 10.
   */
  public ValueMapImpl() {
    this(DEFAULT_SIZE);
  }

  /**
   * Instantiates a new ValueMapImpl with a size of <code>initialSize</code>.
   * @param initialSize the initial size
   */
  public ValueMapImpl(final int initialSize) {
    values = new HashMap<K, V>(initialSize);
  }

  /** {@inheritDoc} */
  public boolean containsValue(final K key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  public boolean isValueNull(final K key) {
    return getValue(key) == null;
  }

  /** {@inheritDoc} */
  public V removeValue(final K key) {
    return values.remove(key);
  }

  /** {@inheritDoc} */
  public V setValue(final K key, final V value) {
    return values.put(key, value);
  }

  /** {@inheritDoc} */
  public V getValue(final K key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  public Collection<K> getValueKeys() {
    return Collections.unmodifiableCollection(values.keySet());
  }

  /** {@inheritDoc} */
  public void clear() {
    values.clear();
  }

  /** {@inheritDoc} */
  public int size() {
    return values.size();
  }

  /** {@inheritDoc} */
  public ValueMap<K, V> getInstance() {
    return new ValueMapImpl<K, V>();
  }

  /** {@inheritDoc} */
  public ValueMap<K, V> getCopy() {
    final ValueMap<K, V> copy = getInstance();
    copy.setAs(this);

    return copy;
  }

  /** {@inheritDoc} */
  public void setAs(final ValueMap<K, V> sourceMap) {
    clear();
    if (sourceMap != null) {
      for (final K entryKey : sourceMap.getValueKeys()) {
        final V value = copyValue(sourceMap.getValue(entryKey));
        setValue(entryKey, value);
      }
    }
  }

  /** {@inheritDoc} */
  public V copyValue(final V value) {
    return value;
  }

  /** {@inheritDoc} */
  public Collection<V> getValues() {
    return Collections.unmodifiableCollection(values.values());
  }

  /**
   * Two ValueChangeMapImpl objects are equal if all current property values are equal.
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
      if (!containsValue(key) || !valuesEqual(otherMap.getValue(key), getValue(key))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 23;
    for (final Object value : getValues()) {
      hash = hash + (value == null ? 0 : value.hashCode());
    }

    return hash;
  }

  protected boolean valuesEqual(final V valueOne, final V valueTwo) {
    return Util.equal(valueOne, valueTwo);
  }
}
