/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.UserException;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * A ComboBoxModel implementation that allows filtering via FilterCriteria objects
 * @see org.jminor.common.model.FilterCriteria
 * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
 */
public class FilteredComboBoxModel implements ComboBoxModel, Refreshable {

  public final Event evtSelectionChanged = new Event();

  private final List<Object> visibleItems = new ArrayList<Object>();
  private final List<Object> filteredItems = new ArrayList<Object>();

  private final Object nullValueItem;
  private Object selectedItem;

  private FilterCriteria filterCriteria;
  private boolean sortContents = false;

  private final Comparator<? super Object> sortComparator;

  private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

  /** Constructs a new FilteredComboBoxModel. */
  public FilteredComboBoxModel() {
    this(false, null);
  }

  public FilteredComboBoxModel(final boolean sortContents, final Object nullValueItem) {
    this(sortContents, nullValueItem, new Comparator<Object>() {
      @SuppressWarnings({"unchecked"})
      public int compare(final Object objectOne, final Object objectTwo) {
        if (objectOne instanceof Comparable && objectTwo instanceof Comparable)
          return ((Comparable) objectOne).compareTo(objectTwo);
        else
          return objectOne.toString().compareTo(objectTwo.toString());
      }
    });
  }

  public FilteredComboBoxModel(final boolean sortContents, final Object nullValueItem,
                               final Comparator<? super Object> sortComparator) {
    this.sortContents = sortContents;
    this.nullValueItem = nullValueItem;
    this.selectedItem = nullValueItem != null ? nullValueItem : null;
    this.sortComparator = sortComparator;
  }

  public boolean isSortContents() {
    return this.sortContents;
  }

  /**
   * @param sort true if the contents of this FilteredComboBoxModel should be sorted
   */
  public void setSortContents(final boolean sort) {
    if (this.sortContents != sort) {
      this.sortContents = sort;
      resetContents();
    }
  }

  /** {@inheritDoc} */
  public void refresh() throws UserException {
    resetContents();
  }

  /**
   * Resets the contents of this model using the values found in <code>contents</code>
   * @param contents the contents to be used by this model
   */
  public final void setContents(final Collection contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      for (final Object object : contents) {
        if (filterCriteria != null && !filterCriteria.include(object))
          filteredItems.add(object);
        else
          visibleItems.add(object);
      }
    }
    if (sortContents)
      Collections.sort(visibleItems, sortComparator);
    if (nullValueItem != null)
      visibleItems.add(0, nullValueItem);

    fireContentsChanged();
  }

  public void resetContents() {
    visibleItems.remove(nullValueItem);
    final Vector<Object> contents = new Vector<Object>(getContents());
    setContents(contents);
  }

  /**
   * @param criteria the FilterCriteria to use
   */
  public void setFilterCriteria(final FilterCriteria criteria) {
    this.filterCriteria = criteria;
    resetContents();
  }

  public void fireContentsChanged() {
    for (final ListDataListener list : listDataListeners.toArray(new ListDataListener[listDataListeners.size()]))
      list.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE));
  }

  public void removeItem(final Object item) {
    if (visibleItems.contains(item))
      visibleItems.remove(item);
    if (filteredItems.contains(item))
      filteredItems.remove(item);

    fireContentsChanged();
  }

  /**
   * @return true if the first item represents a null value, false otherwise
   */
  public boolean isFirstNullValue() {
    return nullValueItem != null;
  }

  /**
   * @return the Object representing the null value, null if none has been specified
   */
  public Object getNullValueItem() {
    return nullValueItem;
  }

  /**
   * @return true if the value representing null is selected, false if none has been specified
   * or it is not selected
   */
  public boolean isNullValueSelected() {
    return selectedItem != null && nullValueItem != null && selectedItem.equals(nullValueItem);
  }

  /** {@inheritDoc} */
  public Object getSelectedItem() {
    return selectedItem;
  }

  /** {@inheritDoc} */
  public void setSelectedItem(final Object item) {
    if (selectedItem == item)
      return;

    selectedItem = item == null ? nullValueItem : item;
    fireContentsChanged();
    evtSelectionChanged.fire();
  }

  /** {@inheritDoc} */
  public void addListDataListener(final ListDataListener l) {
    listDataListeners.add(l);
  }

  /** {@inheritDoc} */
  public void removeListDataListener(final ListDataListener l) {
    listDataListeners.remove(l);
  }

  /** {@inheritDoc} */
  public Object getElementAt(final int index) {
    return visibleItems.get(index);
  }

  /** {@inheritDoc} */
  public int getSize() {
    return visibleItems.size();
  }

  protected List<?> getContents() {
    final List<Object> ret = new ArrayList<Object>(filteredItems);
    ret.addAll(visibleItems);

    return ret;
  }

  protected List<Object> getFilteredItems() {
    return filteredItems;
  }

  protected List<Object> getVisibleItems() {
    return visibleItems;
  }
}