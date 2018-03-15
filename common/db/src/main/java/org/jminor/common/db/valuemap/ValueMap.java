/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.StateObserver;
import org.jminor.common.db.Attribute;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.util.Set;

/**
 * An interface describing an object mapping values to keys, null values are allowed.
 * A ValueMap keeps track of the first value associated with a given key, so that if a value
 * is modified, {@link #getOriginal(Attribute)} returns that original value and
 * {@link #isModified(Attribute)} returns true until the value is either saved via
 * {@link #save(Attribute)} or reverted to its original value via {@link #revert(Attribute)},
 * note that setting the original value manually has the same effect as calling {@link #revert(Attribute)}
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueMap<K extends Attribute, V> extends ValueProvider<K, V>, ValueCollectionProvider<V> {

  /**
   * Maps the given value to the given key, returning the old value if any.
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key
   */
  V put(final K key, final V value);

  /**
   * Removes the given key and value from this value map along with the original value if any.
   * If no value is mapped to the given key, this method has no effect.
   * @param key the key to remove
   * @return the value that was removed, null if no value was found
   */
  V remove(final K key);

  /**
   * Retrieves a string representation of the value mapped to the given key, an empty string is returned
   * in case of null values or if key is not found.
   * @param key the key
   * @return the value mapped to the given key as a string, an empty string if no such mapping exists
   */
  String getAsString(final K key);

  /**
   * Silently removes all values from this map, as in, removes the values
   * without firing value change events
   */
  void clear();

  /**
   * After a call to this method this ValueMap contains the same values and original values as the source map.
   * A null argument to this method clears the destination map of all values and original values.
   * Value change events for affected keys are fired after all values have been set, in no particular order.
   * @param sourceMap the map to copy or null for clearing the destination map
   */
  void setAs(final ValueMap<K, V> sourceMap);

  /**
   * Returns true if a null value is mapped to the given key or the key is not found.
   * @param key the key
   * @return true if the value mapped to the given key is null
   */
  boolean isValueNull(final K key);

  /**
   * Returns true if this ValueMap contains a value for the given key, that value can be null.
   * @param key the key
   * @return true if a value is mapped to this key
   */
  boolean containsKey(final K key);

  /**
   * @return an unmodifiable view of the keys mapping the values in this ValueMap
   */
  Set<K> keySet();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this ValueMap
   */
  Set<K> originalKeySet();

  /**
   * @return the number of values in this map
   */
  int size();

  /**
   * Returns the original value associated with the given key or the current value if it has not been modified.
   * @param key the key for which to retrieve the original value
   * @return the original value
   */
  V getOriginal(final K key);

  /**
   * @return true if one or more values have been modified.
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given key has been modified..
   * @param key the key
   * @return true if the value has changed
   */
  boolean isModified(final K key);

  /**
   * Reverts the value associated with the given key to its original value.
   * If the value has not been modified or the key is not found then calling this method has no effect.
   * @param key the key for which to revert the value
   */
  void revert(final K key);

  /**
   * Reverts all value modifications that have been made.
   * This value map will be unmodified after a call to this method.
   * If no modifications have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param key the key for which to save the value
   */
  void save(final K key);

  /**
   * Saves all the value modifications that have been made.
   * This value map will be unmodified after a call to this method.
   */
  void saveAll();

  /**
   * @return a deep copy of this value map in its original state
   */
  ValueMap<K, V> getOriginalCopy();

  /**
   * @return a new ValueMap instance compatible with this instance
   */
  ValueMap<K, V> newInstance();

  /**
   * @return a deep copy of this value map
   */
  ValueMap<K, V> getCopy();

  /**
   * @return a StateObserver indicating if one or more values in this value map have been modified.
   */
  StateObserver getModifiedObserver();

  /**
   * Returns an EventObserver notified each time a value changes, with a {@link ValueChange} argument.
   * @return an EventObserver notified when a value changes.
   * @see ValueChange
   */
  EventObserver<ValueChange<K, V>> getValueObserver();

  /**
   * Adds a listener notified each time a value changes
   * Adding the same listener multiple times has no effect.
   * @param valueListener the listener
   * @see ValueChange
   */
  void addValueListener(final EventDataListener<ValueChange<K, V>> valueListener);

  /**
   * Removes the given value listener if it has been registered with this value map.
   * @param valueListener the listener to remove
   */
  void removeValueListener(final EventDataListener valueListener);

  /**
   * A validator for ValueMaps
   * @param <K> the type identifying the keys in the value map
   * @param <V> the value map type
   */
  interface Validator<K extends Attribute, V extends ValueMap<K, ?>> {

    /**
     * @param valueMap the value map
     * @param key the key
     * @return true if this value is allowed to be null in the given value map
     */
    boolean isNullable(final V valueMap, final K key);

    /**
     * @param valueMap the value map
     * @return true if the given value map contains only valid values
     */
    boolean isValid(final V valueMap);

    /**
     * Checks if the values in the given value map are valid
     * @param valueMap the value map
     * @throws ValidationException in case of an invalid value
     */
    void validate(final V valueMap) throws ValidationException;

    /**
     * Checks if the value associated with the give key is valid, throws a ValidationException if not
     * @param valueMap the value map to validate
     * @param key the key the value is associated with
     * @throws ValidationException if the given value is not valid for the given key
     */
    void validate(final V valueMap, final K key) throws ValidationException;

    /**
     * Notifies all re-validation listeners that a re-validation is called for, for example
     * due to modified validation settings
     * @see #addRevalidationListener(EventListener)
     */
    void revalidate();

    /**
     * @param listener a listener notified each time a re-validation of all values is required, for example
     * when the validation settings have changed and need to be reflected in the UI
     */
    void addRevalidationListener(final EventListener listener);

    /**
     * @param listener a listener to remove
     */
    void removeRevalidationListener(final EventListener listener);
  }
}
