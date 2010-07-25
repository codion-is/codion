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

  public boolean isValueNull(final K key) {
    return getValue(key) == null;
  }

  public V setValue(final K key, final V value) {
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, value);
    handleValueSet(key, value, previousValue, initialization);

    return previousValue;
  }

  public V getValue(final K key) {
    return values.get(key);
  }

  public String getValueAsString(final K key) {
    final V value = values.get(key);
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  public ValueMap<K, V> getInstance() {
    return new ValueMapImpl<K, V>();
  }

  public V copyValue(final V value) {
    return value;
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
      if (!containsValue(key) || !Util.equal(otherMap.getValue(key), getValue(key))) {
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

  public final boolean containsValue(final K key) {
    return values.containsKey(key);
  }

  public final V removeValue(final K key) {
    if (values.containsKey(key)) {
      final V value = values.remove(key);
      handleValueRemoved(key, value);

      return value;
    }

    return null;
  }

  public final Collection<K> getValueKeys() {
    return Collections.unmodifiableCollection(values.keySet());
  }

  public final void clear() {
    values.clear();
    handleClear();
  }

  public final int size() {
    return values.size();
  }

  public final ValueMap<K, V> getCopy() {
    final ValueMap<K, V> copy = getInstance();
    copy.setAs(this);

    return copy;
  }

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

  public final Collection<V> getValues() {
    return Collections.unmodifiableCollection(values.values());
  }

  protected void handleValueSet(final K key, final V value, final V previousValue, boolean initialization) {}

  protected void handleValueRemoved(final K key, final V value) {}

  protected void handleClear() {}

  protected void handleSetAs(final ValueMap<K, V> sourceMap) {}
}
