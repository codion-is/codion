/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Util;
import org.jminor.common.db.Attribute;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.i18n.Messages;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A default ValueMap implementation.
 * Note that this class is not thread safe.
 * @param <K> the key type
 * @param <V> the value type
 */
public class DefaultValueMap<K extends Attribute, V> implements ValueMap<K, V> {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values;

  /**
   * Holds the original value for keys which values have changed since they were first set.
   */
  private Map<K, V> originalValues = null;

  /**
   * Fired when a value changes, null until initialized by a call to getValueChangedEvent().
   */
  private Event<ValueChange<K, ?>> valueChangedEvent;

  private static final int MAGIC_NUMBER = 23;

  /**
   * Instantiates a new empty instance
   */
  public DefaultValueMap() {
    this(new HashMap<>(), null);
  }

  /**
   * Instantiates a new instance using the given maps for the values and original values respectively.
   * Note that the given map instances are used internally, modifying the contents of those maps outside this
   * DefaultValueMap instance will result in undefined behaviour, to put things politely.
   * @param values the values
   * @param originalValues the originalValues
   */
  protected DefaultValueMap(final Map<K, V> values, final Map<K, V> originalValues) {
    this.values = values == null ? new HashMap<>() : values;
    this.originalValues = originalValues;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValueNull(final K key) {
    return get(key) == null;
  }

  /** {@inheritDoc} */
  @Override
  public V put(final K key, final V value) {
    Objects.requireNonNull(key, "key");
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, value);
    if (!initialization && Objects.equals(previousValue, value)) {
      return value;
    }
    if (!initialization) {
      updateOriginalValue(key, value, previousValue);
    }
    if (valueChangedEvent != null) {
      notifyValueChange(key, value, previousValue, initialization);
    }
    handlePut(key, value, previousValue, initialization);

    return previousValue;
  }

  /** {@inheritDoc} */
  @Override
  public V get(final K key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  @Override
  public String getAsString(final K key) {
    final V value = values.get(key);
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  /**
   * Two DefaultValueMap objects are equal if they contain the
   * same number of values and all their values are equal.
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DefaultValueMap)) {
      return false;
    }

    final DefaultValueMap<K, V> otherMap = (DefaultValueMap<K, V>) obj;
    if (size() != otherMap.size()) {
      return false;
    }

    for (final K key : otherMap.keySet()) {
      if (!containsKey(key) || !Objects.equals(otherMap.get(key), get(key))) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = MAGIC_NUMBER;
    for (final Object value : values()) {
      if (value != null) {
        hash = hash + value.hashCode();
      }
    }

    return hash;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsKey(final K key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public final V remove(final K key) {
    if (values.containsKey(key)) {
      final V value = values.remove(key);
      removeOriginalValue(key);
      handleRemove(key, value);
      if (valueChangedEvent != null) {
        notifyValueChange(key, null, value, false);
      }

      return value;
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    values.clear();
    if (originalValues != null) {
      originalValues = null;
    }
    handleClear();
  }

  /** {@inheritDoc} */
  @Override
  public final int size() {
    return values.size();
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> keySet() {
    return Collections.unmodifiableSet(values.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> originalKeySet() {
    if (originalValues == null) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(originalValues.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<V> values() {
    return Collections.unmodifiableCollection(values.values());
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
  public ValueMap<K, V> newInstance() {
    return new DefaultValueMap<>();
  }

  /** {@inheritDoc} */
  @Override
  public final ValueMap<K, V> getCopy() {
    final ValueMap<K, V> copy = newInstance();
    copy.setAs(this);

    return copy;
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
        final V value = copy(sourceMap.get(key));
        values.put(key, value);
        handlePut(key, value, null, true);
      }
      if (sourceMap.isModified()) {
        originalValues = new HashMap<>();
        for (final K key : sourceMap.originalKeySet()) {
          originalValues.put(key, copy(sourceMap.getOriginal(key)));
        }
      }
    }
    notifyInitialized(affectedKeys);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified(final K key) {
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
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void saveAll() {
    originalValues = null;
  }

  /** {@inheritDoc} */
  @Override
  public final ValueMap<K, V> getOriginalCopy() {
    final ValueMap<K, V> copy = getCopy();
    copy.revertAll();

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final EventInfoListener<ValueChange<K, ?>> valueListener) {
    getValueObserver().addInfoListener(valueListener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final EventInfoListener valueListener) {
    if (valueChangedEvent != null) {
      valueChangedEvent.removeInfoListener(valueListener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getModifiedObserver() {
    final State state = States.state(isModified());
    getValueObserver().addInfoListener(info -> state.setActive(isModified()));

    return state.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, ?>> getValueObserver() {
    return getValueChangedEvent().getObserver();
  }

  protected final void notifyValueChange(final K key, final V value, final V oldValue, final boolean initialization) {
    valueChangedEvent.fire(ValueChanges.valueChange(key, value, oldValue, initialization));
  }

  protected final void setOriginalValue(final K key, final V oldValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(key, oldValue);
  }

  protected final void removeOriginalValue(final K key) {
    if (originalValues != null) {
      originalValues.remove(key);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  /**
   * Returns a deep copy of the given value, immutable values are simply returned.
   * @param value the value to copy
   * @return a deep copy of the given value, or the same instance in case the value is immutable
   */
  protected V copy(final V value) {
    return value;
  }

  /**
   * Called after a value has been put. This base implementation does nothing.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   */
  protected void handlePut(final K key, final V value, final V previousValue, final boolean initialization) {/*Provided for subclasses*/}

  /**
   * Called after a value has been removed from this map. This base implementation does nothing.
   * @param key the key
   * @param value the value that was removed
   */
  protected void handleRemove(final K key, final V value) {/*Provided for subclasses*/}

  /**
   * Called after the value map has been cleared. This base implementation does nothing.
   */
  protected void handleClear() {/*Provided for subclasses*/}

  /**
   * Called after the valueChangeEvent has been initialized, via the first call to {@link #getValueChangedEvent()}
   */
  protected void handleValueChangedEventInitialized() {/*Provided for subclasses*/}

  private void updateOriginalValue(final K key, final V value, final V previousValue) {
    final boolean modified = isModified(key);
    if (modified && Objects.equals(getOriginal(key), value)) {
      removeOriginalValue(key);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(key, previousValue);
    }
  }

  private void notifyInitialized(final Set<K> valueKeys) {
    if (valueChangedEvent != null) {
      for (final K key : valueKeys) {
        final V value = values.get(key);
        valueChangedEvent.fire(ValueChanges.valueChange(key, value, null, true));
      }
    }
  }

  private Event<ValueChange<K, ?>> getValueChangedEvent() {
    if (valueChangedEvent == null) {
      valueChangedEvent = Events.event();
      handleValueChangedEventInitialized();
    }

    return valueChangedEvent;
  }

  /**
   * A default value map validator implementation, which performs basic null validation.
   * @param <K> the type identifying the keys in the value map
   * @param <V> the value map type
   */
  public static class DefaultValidator<K extends Attribute, V extends ValueMap<K, ?>> implements Validator<K, V> {

    private final Event revalidateEvent = Events.event();

    /** {@inheritDoc} */
    @Override
    public boolean isNullable(final V valueMap, final K key) {
      return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(final V valueMap) {
      try {
        validate(valueMap);
        return true;
      }
      catch (final ValidationException e) {
        return false;
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final V valueMap) throws ValidationException {
      Objects.requireNonNull(valueMap, "valueMap");
      for (final K key : valueMap.keySet()) {
        validate(valueMap, key);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final V valueMap, final K key) throws ValidationException {
      Objects.requireNonNull(valueMap, "valueMap");
      if (valueMap.isValueNull(key) && !isNullable(valueMap, key)) {
        throw new NullValidationException(key, Messages.get(Messages.VALUE_MISSING) + ": " + key);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void revalidate() {
      revalidateEvent.fire();
    }

    /** {@inheritDoc} */
    @Override
    public final void addRevalidationListener(final EventListener listener) {
      revalidateEvent.addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public final void removeRevalidationListener(final EventListener listener) {
      revalidateEvent.removeListener(listener);
    }
  }
}
