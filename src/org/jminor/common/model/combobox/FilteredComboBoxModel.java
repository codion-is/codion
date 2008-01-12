/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.IFilterCriteria;
import org.jminor.common.model.IRefreshable;
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

public class FilteredComboBoxModel implements ComboBoxModel, IRefreshable {

  public final Event evtSelectionChanged = new Event("FilteredComboBoxModel.evtSelectionChanged");

  private final Vector<Object> visibleItems = new Vector<Object>();
  private final Vector<Object> filteredItems = new Vector<Object>();

  private final Object nullValueItem;
  private Object selectedItem;

  private IFilterCriteria filterCriteria;
  private boolean sortContents = false;

  private final Comparator<Object> sortComparator = new Comparator<Object>() {
    @SuppressWarnings({"unchecked"})
    public int compare(final Object objectOne, final Object objectTwo) {
      if (objectOne instanceof Comparable && objectTwo instanceof Comparable)
        return ((Comparable) objectOne).compareTo(objectTwo);
      else
        return objectOne.toString().compareTo(objectTwo.toString());
    }
  };

  private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

  /** Constructs a new FilteredComboBoxModel. */
  public FilteredComboBoxModel() {
    this(false, null);
  }

  public FilteredComboBoxModel(final boolean sortContents, final Object nullValueItem) {
    this.sortContents = sortContents;
    this.nullValueItem = nullValueItem;
    this.selectedItem = nullValueItem != null ? nullValueItem : null;
  }

  public boolean sortContents() {
    return this.sortContents;
  }

  /**
   * @param sort Value to set for property 'sortContents'.
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
   * @param contents Value to set for property 'contents'.
   */
  public final void setContents(final Collection contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      for (final Object object : contents) {
        if (filterCriteria != null && !filterCriteria.include(object))
          filteredItems.add(object);
        else {
          visibleItems.add(object);
        }
      }
    }
    if (sortContents)
      Collections.sort(visibleItems, sortComparator);
    if (nullValueItem != null)
      visibleItems.insertElementAt(nullValueItem, 0);

    if (!visibleItems.contains(selectedItem))
      setSelectedItem(null);
    else
      fireContentsChanged();
  }

  public void resetContents() {
    visibleItems.remove(nullValueItem);
    final Vector<Object> contents = new Vector<Object>(getContents());
    setContents(contents);
  }

  /**
   * @param criteria Value to set for property 'filterCriteria'.
   */
  public void setFilterCriteria(final IFilterCriteria criteria) {
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

    resetContents();
  }

  /**
   * @return Value for property 'firstNullValue'.
   */
  public boolean isFirstNullValue() {
    return nullValueItem != null;
  }

  /**
   * @return Value for property 'nullValueItem'.
   */
  public Object getNullValueItem() {
    return nullValueItem;
  }

  /**
   * @return Value for property 'nullValueSelected'.
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

  protected Vector<Object> getFilteredItems() {
    return filteredItems;
  }

  protected Vector<Object> getVisibleItems() {
    return visibleItems;
  }
}