/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * A default FilteredComboBoxModel implementation.
 */
public class DefaultFilteredComboBoxModel<T> implements FilteredComboBoxModel<T> {

  private final Event evtSelectionChanged = new Event();
  private final Event evtFilteringStarted = new Event();
  private final Event evtFilteringDone = new Event();

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
   * Instantiates a new DefaultFilteredComboBoxModel that does not sort its contents and does not include a nullValueItem.
   */
  public DefaultFilteredComboBoxModel() {
    this(null);
  }

  /**
   * Instantiates a new FilteredComboBoxModel.
   * @param nullValueString a string representing a null value, which is shown at the top of the item list
   * @see #isNullValueSelected()
   */
  public DefaultFilteredComboBoxModel(final String nullValueString) {
    this.nullValueString = nullValueString;
    this.sortComparator = initializeItemSortComparator();
  }

  public final void refresh() {
    resetContents();
  }

  public final void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  public final boolean isCleared() {
    return cleared;
  }

  /**
   * Resets the contents of this model using the values found in <code>contents</code>
   * @param contents the contents to be used by this model
   */
  public final void setContents(final Collection<T> contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      visibleItems.addAll(contents);
      filterContents();
      if (nullValueString != null) {
        visibleItems.add(0, null);
      }
    }
    else {
      fireContentsChanged();
    }
    cleared = contents == null;
  }

  public final void filterContents() {
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    final FilterCriteria<T> criteria = getFilterCriteria();
    for (final ListIterator<T> itemIterator = visibleItems.listIterator(); itemIterator.hasNext();) {
      final T item = itemIterator.next();
      if (item != null && !criteria.include(item)) {
        filteredItems.add(item);
        itemIterator.remove();
      }
    }
    if (sortComparator != null) {
      Collections.sort(visibleItems, sortComparator);
    }
    if (selectedItem != null && !visibleItems.contains(selectedItem)) {
      setSelectedItem(null);
    }

    fireContentsChanged();
  }

  public void resetContents() {
    setContents(initializeContents());
  }

  public final List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  public final List<T> getVisibleItems() {
    if (nullValueString == null) {
      return Collections.unmodifiableList(visibleItems);
    }

    return Collections.unmodifiableList(visibleItems.subList(1, getSize() - 1));
  }

  public final void setFilterCriteria(final FilterCriteria filterCriteria) {
    if (filterCriteria == null) {
      this.filterCriteria = acceptAllCriteria;
    }
    else {
      this.filterCriteria = filterCriteria;
    }
    filterContents();
  }

  public FilterCriteria<T> getFilterCriteria() {
    return filterCriteria;
  }

  public final List<T> getAllItems() {
    final List<T> entities = new ArrayList<T>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  public final boolean isVisible(final T item) {
    if (item == null) {
      return nullValueString != null;
    }

    return visibleItems.contains(item);
  }

  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  public final void addItem(final T item) {
    if (getFilterCriteria().include(item)) {
      visibleItems.add(item);
    }
    else {
      filteredItems.add(item);
    }
  }

  public final void removeItem(final T item) {
    if (visibleItems.contains(item)) {
      visibleItems.remove(item);
    }
    if (filteredItems.contains(item)) {
      filteredItems.remove(item);
    }

    fireContentsChanged();
  }

  public final boolean contains(final T item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  public final String getNullValueString() {
    return nullValueString;
  }

  public final void setNullValueString(final String nullValueString) {
    this.nullValueString = nullValueString;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  public final boolean isNullValueSelected() {
    return selectedItem == null && nullValueString != null;
  }

  public final Object getSelectedItem() {
    if (selectedItem == null && nullValueString != null) {
      return nullValueString;
    }

    return selectedItem;
  }

  public void setSelectedItem(final Object anItem) {
    if (Util.equal(selectedItem, anItem)) {
      return;
    }

    if (nullValueString != null && nullValueString.equals(anItem)) {
      selectedItem = null;
    }
    else {
      selectedItem = (T) anItem;
    }
    fireContentsChanged();
    evtSelectionChanged.fire();
  }

  public final void addListDataListener(final ListDataListener l) {
    listDataListeners.add(l);
  }

  public final void removeListDataListener(final ListDataListener l) {
    listDataListeners.remove(l);
  }

  public final Object getElementAt(final int index) {
    final Object element = visibleItems.get(index);
    if (element == null) {
      return nullValueString;
    }

    return element;
  }

  public final int getSize() {
    return visibleItems.size();
  }

  public final Event eventFilteringDone() {
    return evtFilteringDone;
  }

  public final Event eventFilteringStarted() {
    return evtFilteringStarted;
  }

  public final Event eventSelectionChanged() {
    return evtSelectionChanged;
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
   * Initializes the comparator to use when sorting the items in this model,
   * if the model should not be sorted simply override and return null.
   * @return the Comparator to use when sorting the contents of this model.
   */
  protected Comparator<T> initializeItemSortComparator() {
    return new Comparator<T>() {
      private final Collator collator = Collator.getInstance();
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
          return collator.compare(o1.toString(), o2.toString());
        }
      }
    };
  }

  private void fireContentsChanged() {
    final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (final ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }
}