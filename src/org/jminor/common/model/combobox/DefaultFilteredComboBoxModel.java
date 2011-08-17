/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * A default {@link FilteredComboBoxModel} implementation.
 */
public class DefaultFilteredComboBoxModel<T> implements FilteredComboBoxModel<T> {

  private final Event evtSelectionChanged = Events.event();
  private final Event evtFilteringDone = Events.event();

  private final FilterCriteria<T> acceptAllCriteria = new FilterCriteria.AcceptAllCriteria<T>();

  private final List<T> visibleItems = new ArrayList<T>();
  private final List<T> filteredItems = new ArrayList<T>();

  /**
   * set during setContents
   */
  private boolean cleared = true;

  private T selectedItem = null;
  private String nullValueString;
  private FilterCriteria<T> filterCriteria = acceptAllCriteria;

  private final Comparator<? super T> sortComparator;

  private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

  /**
   * Instantiates a new DefaultFilteredComboBoxModel, without a nullValueString.
   * The model contents are sorted automatically.
   */
  public DefaultFilteredComboBoxModel() {
    this(null);
  }

  /**
   * Instantiates a new DefaultFilteredComboBoxModel with the given nullValueString.
   * The model contents are sorted automatically.
   * @param nullValueString a string representing a null value, which is shown at the top of the item list
   * @see #isNullValueSelected()
   */
  public DefaultFilteredComboBoxModel(final String nullValueString) {
    this.nullValueString = nullValueString;
    this.sortComparator = new SortComparator<T>(nullValueString);
  }

  /** {@inheritDoc} */
  public final void refresh() {
    setContents(initializeContents());
  }

  /** {@inheritDoc} */
  public final void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  /** {@inheritDoc} */
  public final boolean isCleared() {
    return cleared;
  }

  /** {@inheritDoc} */
  public final void setContents(final Collection<T> contents) {
    if (contents == null || !contents.contains(selectedItem)) {
      setSelectedItem(null);
    }
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      visibleItems.addAll(contents);
      filterContents();
      if (nullValueString != null) {
        visibleItems.add(0, null);
      }
    }
    fireContentsChanged();
    cleared = contents == null;
  }

  /** {@inheritDoc} */
  public final void filterContents() {
    try {
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      for (final ListIterator<T> itemIterator = visibleItems.listIterator(); itemIterator.hasNext();) {
        final T item = itemIterator.next();
        if (item != null && !filterCriteria.include(item)) {
          filteredItems.add(item);
          itemIterator.remove();
        }
      }
      sort(visibleItems);
      if (selectedItem != null && !visibleItems.contains(selectedItem)) {
        setSelectedItem(null);
      }

      fireContentsChanged();
    }
    finally {
      evtFilteringDone.fire();
    }
  }

  /** {@inheritDoc} */
  public final List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  /** {@inheritDoc} */
  public final List<T> getVisibleItems() {
    if (nullValueString == null) {
      return Collections.unmodifiableList(visibleItems);
    }

    return Collections.unmodifiableList(visibleItems.subList(1, getSize() - 1));
  }

  /** {@inheritDoc} */
  public final void setFilterCriteria(final FilterCriteria<T> filterCriteria) {
    if (filterCriteria == null) {
      this.filterCriteria = acceptAllCriteria;
    }
    else {
      this.filterCriteria = filterCriteria;
    }
    filterContents();
  }

  /** {@inheritDoc} */
  public final FilterCriteria<T> getFilterCriteria() {
    return filterCriteria;
  }

  /** {@inheritDoc} */
  public final List<T> getAllItems() {
    final List<T> entities = new ArrayList<T>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  /** {@inheritDoc} */
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  /** {@inheritDoc} */
  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  public final boolean isVisible(final T item) {
    if (item == null) {
      return nullValueString != null;
    }

    return visibleItems.contains(item);
  }

  /** {@inheritDoc} */
  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  /** {@inheritDoc} */
  public final void addItem(final T item) {
    if (filterCriteria.include(item)) {
      visibleItems.add(item);
    }
    else {
      filteredItems.add(item);
    }
  }

  /** {@inheritDoc} */
  public final void removeItem(final T item) {
    if (visibleItems.contains(item)) {
      visibleItems.remove(item);
    }
    if (filteredItems.contains(item)) {
      filteredItems.remove(item);
    }

    fireContentsChanged();
  }

  /** {@inheritDoc} */
  public final boolean contains(final T item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  /** {@inheritDoc} */
  public final String getNullValueString() {
    return nullValueString;
  }

  /** {@inheritDoc} */
  public final void setNullValueString(final String nullValueString) {
    this.nullValueString = nullValueString;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  /** {@inheritDoc} */
  public final boolean isNullValueSelected() {
    if (selectedItem instanceof String && nullValueString == null) {
      return ((String) selectedItem).isEmpty();
    }

    return selectedItem == null && nullValueString != null;
  }

  /** {@inheritDoc} */
  public final T getSelectedValue() {
    if (isNullValueSelected()) {
      return null;
    }

    return selectedItem;
  }

  /**
   * @return the selected item, N.B. this can include the <code>nullValueString</code>
   * in case it has been set, {@link #getSelectedValue()} is usually what you want
   */
  public final Object getSelectedItem() {
    if (selectedItem == null && nullValueString != null) {
      return nullValueString;
    }

    return selectedItem;
  }

  /** {@inheritDoc} */
  public final void setSelectedItem(final Object anItem) {
    final Object toSelect = translateSelectionItem(anItem);
    if (vetoSelectionChange(toSelect)) {
      return;
    }
    if (Util.equal(selectedItem, toSelect)) {
      return;
    }

    if (nullValueString != null && nullValueString.equals(toSelect)) {
      selectedItem = null;
    }
    else {
      //noinspection unchecked
      selectedItem = (T) toSelect;
    }
    fireContentsChanged();
    evtSelectionChanged.fire();
  }

  /** {@inheritDoc} */
  public final void addListDataListener(final ListDataListener l) {
    listDataListeners.add(l);
  }

  /** {@inheritDoc} */
  public final void removeListDataListener(final ListDataListener l) {
    listDataListeners.remove(l);
  }

  /** {@inheritDoc} */
  public final Object getElementAt(final int index) {
    final Object element = visibleItems.get(index);
    if (element == null) {
      return nullValueString;
    }

    return element;
  }

  /** {@inheritDoc} */
  public final int getSize() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  public final void addFilteringListener(final ActionListener listener) {
    evtFilteringDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeFilteringListener(final ActionListener listener) {
    evtFilteringDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addSelectionListener(final ActionListener listener) {
    evtSelectionChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSelectionListener(final ActionListener listener) {
    evtSelectionChanged.removeListener(listener);
  }

  /**
   * @return a List containing the items to be shown in this combo box model,
   * by default it simply returns a list containing the items currently contained in the model,
   * both filtered and visible, excluding the null value.
   */
  protected List<T> initializeContents() {
    final List<T> contents = new ArrayList<T>(visibleItems);
    if (nullValueString != null) {
      contents.remove(null);
    }
    contents.addAll(filteredItems);

    return contents;
  }

  /**
   * @param item the item to be selected
   * @return true if the selection change is ok, false if it should be vetoed
   */
  protected boolean vetoSelectionChange(final Object item) {
    return false;
  }

  /**
   * @param item the item to be selected
   * @return the actual item to select
   */
  protected Object translateSelectionItem(final Object item) {
    return item;
  }

  /**
   * Sorts the items in the given list, used when sorting
   * the contents of this model. This method is responsible for calling
   * {@link #fireContentsChanged()} when done sorting.
   * @param items the items to sort
   */
  protected void sort(final List<T> items) {
    Collections.sort(items, sortComparator);
    fireContentsChanged();
  }

  /**
   * Fires a {@link ListDataEvent#CONTENTS_CHANGED} event on all registered listeners
   */
  protected final void fireContentsChanged() {
    final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (final ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }

  private static final class SortComparator<T> implements Comparator<T>, Serializable {
    private static final long serialVersionUID = 1;

    private final String nullValueString;
    private final Comparator comparator = Util.getSpaceAwareCollator();

    SortComparator(final String nullValueString) {
      this.nullValueString = nullValueString;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public int compare(final T o1, final T o2) {
      if (o1 == null && o2 == null) {
        return 0;
      }
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      if (nullValueString != null) {
        if (o1.equals(nullValueString)) {
          return -1;
        }
        if (o2.equals(nullValueString)) {
          return 1;
        }
      }
      if (o1 instanceof Comparable && o2 instanceof Comparable) {
        return ((Comparable) o1).compareTo(o2);
      }
      else {
        return comparator.compare(o1, o2);
      }
    }
  }
}