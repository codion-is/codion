/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.Util;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A ComboBoxModel implementation that allows filtering via FilterCriteria objects.
 * @see org.jminor.common.model.FilterCriteria
 * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
 */
public class FilteredComboBoxModel implements ComboBoxModel, Refreshable {

  private final Event evtSelectionChanged = new Event();

  private final List<Object> visibleItems = new ArrayList<Object>();
  private final List<Object> filteredItems = new ArrayList<Object>();

  private Object selectedItem;
  private String nullValueString;

  private FilterCriteria filterCriteria;
  private boolean sortContents = false;

  private final Comparator<? super Object> sortComparator;

  private final List<ListDataListener> listDataListeners = new ArrayList<ListDataListener>();

  /**
   * Instantiates a new FilteredComboBoxModel that does not sort its contents and does not include a nullValueItem.
   */
  public FilteredComboBoxModel() {
    this(false, null);
  }

  /**
   * Instantiates a new FilteredComboBoxModel.
   * @param sortContents if true then the contents of this model are sorted on refresh
   * @param nullValueString a string representing a null value, which is shown at the top of the item list
   * @see #isNullValueSelected()
   */
  public FilteredComboBoxModel(final boolean sortContents, final String nullValueString) {
    this.sortContents = sortContents;
    this.nullValueString = nullValueString;
    this.selectedItem = nullValueString != null ? nullValueString : null;
    this.sortComparator = sortContents ? initializeComparator() : null;
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
  public void refresh() {
    resetContents();
  }

  public void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  /**
   * Resets the contents of this model using the values found in <code>contents</code>
   * @param contents the contents to be used by this model
   */
  public final void setContents(final Collection<?> contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (nullValueString != null) {
      visibleItems.add(nullValueString);
    }
    if (contents != null) {
      visibleItems.addAll(contents);
    }
    filterContents();
  }

  /**
   * Filters the contents of this model according to the <code>include</code> method
   * @see #include(Object)
   */
  public void filterContents() {
    final List<Object> allItems = new ArrayList<Object>(visibleItems);
    allItems.addAll(filteredItems);
    visibleItems.clear();
    filteredItems.clear();
    for (final Object item : allItems) {
      if (include(item)) {
        visibleItems.add(item);
      }
      else {
        filteredItems.add(item);
      }
    }
    if (sortContents) {
      Collections.sort(visibleItems, sortComparator);
    }

    fireContentsChanged();
  }

  public void resetContents() {
    setContents(getContents());
  }

  /**
   * @param criteria the FilterCriteria to use
   */
  public void setFilterCriteria(final FilterCriteria criteria) {
    this.filterCriteria = criteria;
    filterContents();
  }

  public void fireContentsChanged() {
    final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (final ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }

  public void removeItem(final Object item) {
    if (visibleItems.contains(item)) {
      visibleItems.remove(item);
    }
    if (filteredItems.contains(item)) {
      filteredItems.remove(item);
    }

    fireContentsChanged();
  }

  /**
   * @return the String representing the null value, null if none has been specified
   */
  public String getNullValueString() {
    return nullValueString;
  }

  /**
   * Sets the nullValueItem, a refresh is required for it to show up
   * @param nullValueString a String representing a null value
   */
  public void setNullValueString(final String nullValueString) {
    this.nullValueString = nullValueString;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  /**
   * @return true if a value representing null is selected
   */
  public boolean isNullValueSelected() {
    return selectedItem != null && nullValueString != null && selectedItem.equals(nullValueString);
  }

  /** {@inheritDoc} */
  public Object getSelectedItem() {
    return selectedItem;
  }

  /** {@inheritDoc} */
  public void setSelectedItem(final Object anItem) {
    if (Util.equal(selectedItem, anItem)) {
      return;
    }

    selectedItem = anItem == null ? nullValueString : anItem;
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

  public Event eventSelectionChanged() {
    return evtSelectionChanged;
  }

  /**
   * @return a List containing the items to be shown in this combo box model,
   * by default it simply returns a list containing the items currently contained in the model,
   * excluding the null value string if one is specified.
   */
  protected List<?> getContents() {
    final List<Object> contents = new ArrayList<Object>(visibleItems);
    if (nullValueString != null) {
      contents.remove(nullValueString);
    }
    contents.addAll(filteredItems);

    return contents;
  }

  protected List<Object> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

    /**
   * @return a List containing the visibble items in this combo box model,
   * excluding the null value string if one is specified.
   */
  protected List<Object> getVisibleItems() {
    if (nullValueString == null) {
      return Collections.unmodifiableList(visibleItems);
    }

    return Collections.unmodifiableList(visibleItems.subList(1, getSize() - 1));
  }

  /**
   * Returns true if the given object should be included or filtered out of this model
   * @param object the object
   * @return true if the object should be included
   * @see #setFilterCriteria(org.jminor.common.model.FilterCriteria)
   */
  @SuppressWarnings({"unchecked"})
  protected boolean include(final Object object) {
    return filterCriteria == null || filterCriteria.include(object);
  }

  protected Comparator<Object> initializeComparator() {
    return new Comparator<Object>() {
      private final Collator collator = Collator.getInstance();
      @SuppressWarnings({"unchecked"})
      public int compare(final Object o1, final Object o2) {
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