/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;

import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.Comparator;

/**
 * A ComboBoxModel extension that allows filtering via FilterCriteria objects.
 * @see org.jminor.common.model.FilterCriteria
 * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
 */
public interface FilteredComboBoxModel<T> extends FilteredModel<T>, ComboBoxModel<T>, Refreshable {

  /**
   * @param listener a listener notified each time the selection changes
   */
  void addSelectionListener(final EventListener listener);

  /**
   * @param listener a selection listener to remove
   */
  void removeSelectionListener(final EventListener listener);

  /**
   * Resets the contents of this model using the values found in <code>contents</code>,
   * if contents is null then the model is considered to be cleared.
   * @param contents the contents to display in this combo box model
   * @see #isCleared()
   */
  void setContents(final Collection<? extends T> contents);

  /**
   * Adds the given item to this model, respecting the sorting order if specified
   * @param item the item to add
   */
  void addItem(final T item);

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  void removeItem(final T item);

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
   * @see #setNullValue(Object)
   */
  boolean isNullValueSelected();

  /**
   * @return the selected item, null in case the value representing null is selected
   * @see #setNullValue(Object)
   */
  T getSelectedValue();

  /**
   * Sets the Comparator used when sorting the items visible in this model and sorts the model accordingly.
   * This Comparator must take into account the null value if a <code>nullValueString</code> is specified.
   * If a null <code>sortComparator</code> is provided no sorting will be performed.
   * @param sortComparator the Comparator, null if the contents of this model should not be sorted
   */
  void setSortComparator(Comparator<? super T> sortComparator);

  /**
   * @return the Comparator used when sorting the contents of this model
   */
  Comparator<? super T> getSortComparator();

  /**
   * Sets the value which should represent a null value, a refresh is required for it to show up
   * @param nullValue a value which is used to represent a null value
   */
  void setNullValue(final T nullValue);

  /**
   * @return the value representing the null value, null if no such value has been specified
   */
  T getNullValue();

  /**
   * Specifies whether or not filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * By default this is true.
   * @param value if true then the selected item is changed when it is filtered out,
   */
  void setFilterSelectedItem(final boolean value);

  /**
   * Specifies whether or not filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * By default this is true.
   * @return true if the selected item is changed when it is filtered out
   */
  boolean isFilterSelectedItem();
}