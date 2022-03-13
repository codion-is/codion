/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.combobox;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.properties.PropertyValue;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * A ComboBoxModel that allows filtering via Predicate objects.
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
  PropertyValue<String> COMBO_BOX_NULL_VALUE_ITEM = Configuration.stringValue("is.codion.common.model.combobox.comboBoxNullValueItem")
          .defaultValue("-")
          .build();

  /**
   * @param listener a listener notified each time the selection changes
   */
  void addSelectionListener(EventDataListener<T> listener);

  /**
   * @param listener a selection listener to remove
   */
  void removeSelectionListener(EventDataListener<T> listener);

  /**
   * @param listener a listener to be notified each time this model has been successfully refreshed
   * @see #refresh()
   */
  void addRefreshListener(EventListener listener);

  /**
   * @param listener the listener to remove
   * @see #refresh()
   */
  void removeRefreshListener(EventListener listener);

  /**
   * Resets the contents of this model using the values found in {@code contents},
   * if contents is null then the model is considered to be cleared.
   * @param contents the contents to display in this combo box model
   * @see #isCleared()
   */
  void setContents(Collection<T> contents);

  /**
   * Adds the given item to this model, respecting the sorting order if specified
   * @param item the item to add
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
   * Returns true if the value representing null is selected, false if no such value has been
   * specified or if it is not selected.
   * @return true if the value representing null is selected, false otherwise
   * @see #setNullString(String)
   */
  boolean isNullValueSelected();

  /**
   * @return the selected item, null in case the value representing null is selected
   * @see #setNullString(String)
   * @see #isNullValueSelected()
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
   * Sets the value which should represent a null value, a refresh is required for it to show up
   * @param nullString a value which is used to represent a null value
   */
  void setNullString(String nullString);

  /**
   * @return the value representing the null value, null if no such value has been specified
   */
  String getNullString();

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
  Object getElementAt(int index);

  /**
   * @return the selected item
   */
  Object getSelectedItem();

  /**
   * Refreshes the items in this combo box model
   * @see #addRefreshListener(EventListener)
   */
  void refresh();

  /**
   * Clears all items from this combo box model
   */
  void clear();
}