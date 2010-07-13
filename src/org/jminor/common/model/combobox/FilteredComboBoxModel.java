/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;

import javax.swing.ComboBoxModel;

/**
 * A ComboBoxModel extension that allows filtering via FilterCriteria objects.
 * @see org.jminor.common.model.FilterCriteria
 * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
 */
public interface FilteredComboBoxModel<T> extends ComboBoxModel, Refreshable, FilteredModel<T> {

  Event eventSelectionChanged();

  /**
   * Returns true if the given item is visible in this combo box model,
   * null values are considered visible if a <code>nullValueString</code>
   * has been specified.
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(final T item);

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  void removeItem(final T item);

  /**
   * Returns true if this combo box model contains the given item, visible or filtered.
   * A null value is considered contained in the model if a <code>nullValueString</code>
   * has been specified.
   * @param item the item
   * @return true if this combo box model contains the item
   */
  boolean contains(final T item);

  /**
   * @return true if the model data needs to be refreshed
   */
  boolean isCleared();

  boolean isSortContents();

  /**
   * @param sort true if the contents of this FilteredComboBoxModel should be sorted
   */
  void setSortContents(final boolean sort);

  /**
   * @return true if a value representing null is selected
   */
  boolean isNullValueSelected();

  /**
   * Sets the nullValueItem, a refresh is required for it to show up
   * @param nullValueString a String representing a null value
   */
  void setNullValueString(final String nullValueString);

  /**
   * @return the String representing the null value, null if none has been specified
   */
  String getNullValueString();
}