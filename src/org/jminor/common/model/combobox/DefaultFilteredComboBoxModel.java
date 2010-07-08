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
  private boolean clear = true;

  private T selectedItem = null;
  private String nullValueString;
  private FilterCriteria<T> filterCriteria = acceptAllCriteria;
  private boolean sortContents = true;

  private final Comparator<? super T> sortComparator;

  private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

  /**
   * Instantiates a new DefaultFilteredComboBoxModel that does not sort its contents and does not include a nullValueItem.
   */
  public DefaultFilteredComboBoxModel() {
    this(true, null);
  }

  /**
   * Instantiates a new FilteredComboBoxModel.
   * @param sortContents if true then the contents of this model are sorted on refresh
   * @param nullValueString a string representing a null value, which is shown at the top of the item list
   * @see #isNullValueSelected()
   */
  public DefaultFilteredComboBoxModel(final boolean sortContents, final String nullValueString) {
    this.sortContents = sortContents;
    this.nullValueString = nullValueString;
    this.sortComparator = sortContents ? initializeComparator() : null;
  }

  public boolean isSortContents() {
    return this.sortContents;
  }

  public void setSortContents(final boolean sort) {
    if (this.sortContents != sort) {
      this.sortContents = sort;
      resetContents();
    }
  }

  public void refresh() {
    resetContents();
  }

  public void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  public boolean isClear() {
    return clear;
  }

  /**
   * Resets the contents of this model using the values found in <code>contents</code>
   * @param contents the contents to be used by this model
   */
  public final void setContents(final Collection<T> contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (nullValueString != null) {
      visibleItems.add(null);
    }
    if (contents != null) {
      visibleItems.addAll(contents);
    }
    clear = contents == null;
    filterContents();
  }

  public void filterContents() {
    final List<T> allItems = new ArrayList<T>(visibleItems);
    allItems.addAll(filteredItems);
    visibleItems.clear();
    filteredItems.clear();
    for (final T item : allItems) {
      if (item == null || getFilterCriteria().include(item)) {
        visibleItems.add(item);
      }
      else {
        filteredItems.add(item);
      }
    }
    if (sortContents) {
      Collections.sort(visibleItems, sortComparator);
    }
    if (selectedItem != null && !visibleItems.contains(selectedItem)) {
     setSelectedItem(null);
    }

    fireContentsChanged();
  }

  public void resetContents() {
    setContents(getContents());
  }

  public void setFilterCriteria(final FilterCriteria filterCriteria) {
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

  public Event eventFilteringDone() {
    return evtFilteringDone;
  }

  public Event eventFilteringStarted() {
    return evtFilteringStarted;
  }

  public void fireContentsChanged() {
    final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (final ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }

  public void removeItem(final T item) {
    if (visibleItems.contains(item)) {
      visibleItems.remove(item);
    }
    if (filteredItems.contains(item)) {
      filteredItems.remove(item);
    }

    fireContentsChanged();
  }

  public boolean isVisible(final T item) {
    if (item == null) {
      return nullValueString != null;
    }
    return visibleItems.contains(item);
  }

  public boolean contains(final T item) {
    if (item == null) {
      return nullValueString != null;
    }
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  public String getNullValueString() {
    return nullValueString;
  }

  public void setNullValueString(final String nullValueString) {
    this.nullValueString = nullValueString;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  public boolean isNullValueSelected() {
    return selectedItem == null && nullValueString != null;
  }

  public Object getSelectedItem() {
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

  public void addListDataListener(final ListDataListener l) {
    listDataListeners.add(l);
  }

  public void removeListDataListener(final ListDataListener l) {
    listDataListeners.remove(l);
  }

  public Object getElementAt(final int index) {
    final Object element = visibleItems.get(index);
    if (element == null) {
      return nullValueString;
    }

    return element;
  }

  public int getSize() {
    return visibleItems.size();
  }

  public Event eventSelectionChanged() {
    return evtSelectionChanged;
  }

  /**
   * @return a List containing the items to be shown in this combo box model,
   * by default it simply returns a list containing the items currently contained in the model,
   * excluding the null value string if one is specified.
   */
  protected List<T> getContents() {
    final List<T> contents = new ArrayList<T>(visibleItems);
    if (nullValueString != null) {
      contents.remove(null);
    }
    contents.addAll(filteredItems);

    return contents;
  }

  protected List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

    /**
   * @return a List containing the visibble items in this combo box model,
   * excluding the null value string if one is specified.
   */
  protected List<T> getVisibleItems() {
    if (nullValueString == null) {
      return Collections.unmodifiableList(visibleItems);
    }

    return Collections.unmodifiableList(visibleItems.subList(1, getSize() - 1));
  }

  protected Comparator<T> initializeComparator() {
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
}