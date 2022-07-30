/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.combobox;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.properties.PropertyValue;
import is.codion.common.value.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel that allows filtering via a Predicate.
 * @param <T> the type of the combo box model elements
 * @see #setIncludeCondition(Predicate)
 */
public interface FilteredComboBoxModel<T> extends FilteredModel<T> {

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used.<br>
   * Value type: String<br>
   * Default value: -
   */
  PropertyValue<String> COMBO_BOX_NULL_VALUE_ITEM = Configuration.stringValue("is.codion.common.model.combobox.comboBoxNullValueItem", "-");

  /**
   * @param listener a listener notified each time the selection changes
   */
  void addSelectionListener(EventDataListener<T> listener);

  /**
   * @param listener a selection listener to remove
   */
  void removeSelectionListener(EventDataListener<T> listener);

  /**
   * Resets the contents of this model using the values found in {@code contents},
   * if contents is null then the model is considered to be cleared.
   * @param contents the contents to display in this combo box model
   * @see #isCleared()
   */
  void setContents(Collection<T> contents);

  /**
   * Adds the given item to this model, respecting the sorting order if specified.
   * If this model already contains the item, calling this method has no effect.
   * Note that if the item does not satisfy the include condition, it will be filtered right away.
   * @param item the item to add
   * @see #setIncludeCondition(Predicate)
   */
  void addItem(T item);

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  void removeItem(T item);

  /**
   * Replaces the given item in this combo box model
   * @param item the item to replace
   * @param replacement the replacement item
   */
  void replaceItem(T item, T replacement);

  /**
   * @return true if the model data has been cleared and needs to be refreshed
   */
  boolean isCleared();

  /**
   * @return true if no value is selected or if the value representing null is selected
   */
  boolean isSelectionEmpty();

  /**
   * Returns true if this model contains null and it is selected.
   * @return true if this model contains null and it is selected, false otherwise
   * @see #isIncludeNull()
   */
  boolean isNullSelected();

  /**
   * @return the selected value, null in case the value representing null is selected
   * @see #isNullSelected()
   */
  T getSelectedValue();

  /**
   * Sets the Comparator used when sorting the items visible in this model and sorts the model accordingly.
   * This Comparator must take into account the null value if a {@code nullValueString} is specified.
   * If a null {@code sortComparator} is provided no sorting will be performed.
   * @param sortComparator the Comparator, null if the contents of this model should not be sorted
   */
  void setSortComparator(Comparator<T> sortComparator);

  /**
   * @return the Comparator used when sorting the contents of this model
   */
  Comparator<T> getSortComparator();

  /**
   * @param includeNull if true then a null value is included as the first item
   * @see #setIncludeNull(String, Class)
   */
  void setIncludeNull(boolean includeNull);

  /**
   * Configures this model to include a null value and sets the String to use as its caption.
   * Note that this is restricted to {@code itemClass} values that are interfaces, with String as the one exception.
   * @param caption the null value caption
   * @param itemClass the class representing the type contained in this combo box model
   */
  void setIncludeNull(String caption, Class<T> itemClass);

  /**
   * @return true if a null value is included
   */
  boolean isIncludeNull();

  /**
   * Specifies whether filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * This is true by default.
   * @param filterSelectedItem if true then the selected item is changed when it is filtered out,
   */
  void setFilterSelectedItem(boolean filterSelectedItem);

  /**
   * Specifies whether filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * This is true by default.
   * @return true if the selected item is changed when it is filtered out
   */
  boolean isFilterSelectedItem();

  /**
   * @param item the item to select
   */
  void setSelectedItem(Object item);

  /**
   * @return the number of visible items in this model
   */
  int getSize();

  /**
   * @param index the index
   * @return the item at the given index
   */
  T getElementAt(int index);

  /**
   * @return the selected item
   */
  Object getSelectedItem();

  /**
   * @param finder responsible for finding the item to select
   * @return a value linked to the selected item via the given finder instance
   * @param <V> the value type
   */
  <V> Value<V> selectorValue(Finder<T, V> finder);

  /**
   * Refreshes the items in this combo box model.
   * If run on the Event Dispatch Thread the refresh happens asynchronously.
   * @throws RuntimeException in case of an exception when running refresh synchronously, as in, not on the Event Dispatch Thread
   * @see #addRefreshFailedListener(EventDataListener)
   */
  void refresh();

  /**
   * Clears all items from this combo box model
   */
  void clear();

  /**
   * Responsible for finding an item of type {@link T} by a single value of type {@link V}.
   * @param <T> the combo box model item type
   * @param <V> the type of the value to search by
   */
  interface Finder<T, V> {

    /**
     * Returns the value from the given item to use when searching
     * @param item the item, never null
     * @return the value associated with the given item
     */
    V getValue(T item);

    /**
     * Returns the {@link Predicate} to use when searching for the given value
     * @param value the value to search for, never null
     * @return a {@link Predicate} based on the given value
     */
    Predicate<T> getPredicate(V value);

    /**
     * Returns the first item in the given collection containing the given {@code value}. Only called for non-null {@code value}s.
     * @param items the items to search
     * @param value the value to search for, never null
     * @return the first item in the given list containing the given value, null if none is found.
     */
    default T findItem(Collection<T> items, V value) {
      requireNonNull(value);

      return requireNonNull(items).stream()
              .filter(getPredicate(value))
              .findFirst()
              .orElse(null);
    }
  }
}