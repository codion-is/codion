/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.FilteredModel;
import org.jminor.common.model.Refreshable;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * A ComboBoxModel extension that allows filtering via FilterCriteria objects.
 * @see org.jminor.common.model.FilterCriteria
 * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
 */
public interface FilteredComboBoxModel<T> extends FilteredModel<T>, ComboBoxModel, Refreshable {

  /**
   * @param listener a listener notified each time the selection changes
   */
  void addSelectionListener(final ActionListener listener);

  /**
   * @param listener a selection listener to remove
   */
  void removeSelectionListener(final ActionListener listener);

  /**
   * Resets the contents of this model using the values found in <code>contents</code>
   * @param contents the contents to display in this combo box model
   */
  void setContents(final Collection<T> contents);

  /**
   * @param item the item to add
   */
  void addItem(final T item);

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  void removeItem(final T item);

  /**
   * @return true if the model data needs to be refreshed
   */
  boolean isCleared();

  /**
   * @return true if a value representing null is selected
   */
  boolean isNullValueSelected();

  /**
   * @return the selected item, null in case the <code>nullValueString</code> is selected
   */
  T getSelectedValue();

  /**
   * Sets the nullValueItem, a refresh is required for it to show up
   * @param nullValueString a String representing a null value
   */
  void setNullValueString(final String nullValueString);

  /**
   * @return the String representing the null value, null if none has been specified
   */
  String getNullValueString();

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