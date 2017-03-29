/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.TextUtil;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.combobox.FilteredComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A default {@link FilteredComboBoxModel} implementation.
 * @param <T> the type of values in this combo box model
 */
public class SwingFilteredComboBoxModel<T> implements FilteredComboBoxModel<T>, ComboBoxModel<T> {

  private static final FilterCondition ACCEPT_ALL_CONDITION = new FilterCondition.AcceptAllCondition();

  private final Event<T> selectionChangedEvent = Events.event();
  private final Event filteringDoneEvent = Events.event();

  private final List<T> visibleItems = new ArrayList<>();
  private final List<T> filteredItems = new ArrayList<>();

  /**
   * set during setContents
   */
  private boolean cleared = true;

  private Comparator<T> sortComparator;
  private T selectedItem = null;
  private T nullValue;
  private FilterCondition<T> filterCondition = ACCEPT_ALL_CONDITION;
  private boolean filterSelectedItem = true;

  /**
   * Due to a java.util.ConcurrentModificationException in OSX
   */
  private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

  /**
   * Instantiates a new SwingFilteredComboBoxModel, without a nullValueString.
   * The model contents are sorted automatically.
   */
  public SwingFilteredComboBoxModel() {
    this(null);
  }

  /**
   * Instantiates a new SwingFilteredComboBoxModel, without a nullValueString.
   * The model contents are sorted automatically with a default collation based comparator.
   * @param nullValue a value representing a null value, which is shown at the top of the item list
   */
  public SwingFilteredComboBoxModel(final T nullValue) {
    this(nullValue, new SortComparator<>(nullValue));
  }

  /**
   * Instantiates a new SwingFilteredComboBoxModel with the given nullValueString.
   * @param nullValue a value representing a null value, which is shown at the top of the item list
   * @param sortComparator the Comparator used to sort the contents of this combo box model, if null then
   * the contents are not sorted
   * @see #isNullValueSelected()
   */
  public SwingFilteredComboBoxModel(final T nullValue, final Comparator<T> sortComparator) {
    this.nullValue = nullValue;
    this.sortComparator = sortComparator;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    setContents(initializeContents());
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCleared() {
    return cleared;
  }

  /** {@inheritDoc} */
  @Override
  public final void setContents(final Collection<T> contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      visibleItems.addAll(contents);
      if (nullValue != null) {
        visibleItems.add(0, null);
      }
    }
    filterContents();
    cleared = contents == null;
  }

  /** {@inheritDoc} */
  @Override
  public final void filterContents() {
    try {
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      if (!visibleItems.isEmpty()) {
        for (final ListIterator<T> itemIterator = visibleItems.listIterator(); itemIterator.hasNext();) {
          final T item = itemIterator.next();
          if (item != null && !filterCondition.include(item)) {
            filteredItems.add(item);
            itemIterator.remove();
          }
        }
        sortVisibleItems();
      }
      if (selectedItem != null && !visibleItems.contains(selectedItem) && filterSelectedItem) {
        setSelectedItem(null);
      }
      else {
        fireContentsChanged();
      }
    }
    finally {
      filteringDoneEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<T> getVisibleItems() {
    if (nullValue == null) {
      return Collections.unmodifiableList(visibleItems);
    }

    return Collections.unmodifiableList(visibleItems.subList(1, getSize()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  /** {@inheritDoc} */
  @Override
  public final List<T> getAllItems() {
    final List<T> entities = new ArrayList<>(getVisibleItems());
    entities.addAll(filteredItems);

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterCondition(final FilterCondition<T> filterCondition) {
    if (filterCondition == null) {
      this.filterCondition = ACCEPT_ALL_CONDITION;
    }
    else {
      this.filterCondition = filterCondition;
    }
    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final FilterCondition<T> getFilterCondition() {
    return filterCondition;
  }

  /** {@inheritDoc} */
  @Override
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  /** {@inheritDoc} */
  @Override
  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isVisible(final T item) {
    if (item == null) {
      return nullValue != null;
    }

    return visibleItems.contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final void addItem(final T item) {
    if (filterCondition.include(item)) {
      visibleItems.add(item);
      sortVisibleItems();
    }
    else {
      filteredItems.add(item);
    }
  }

  /** {@inheritDoc} */
  @Override
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
  @Override
  public final boolean contains(final T item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public final Comparator<T> getSortComparator() {
    return sortComparator;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSortComparator(final Comparator<T> sortComparator) {
    this.sortComparator = sortComparator;
    sortVisibleItems();
  }

  /** {@inheritDoc} */
  @Override
  public final T getNullValue() {
    return nullValue;
  }

  /** {@inheritDoc} */
  @Override
  public final void setNullValue(final T nullValue) {
    this.nullValue = nullValue;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullValueSelected() {
    return selectedItem == null && nullValue != null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isSelectionEmpty() {
    return getSelectedValue() == null;
  }

  /** {@inheritDoc} */
  @Override
  public final T getSelectedValue() {
    if (isNullValueSelected()) {
      return null;
    }

    return selectedItem;
  }

  /**
   * @return the selected item, N.B. this can include the {@code nullValue}
   * in case it has been set, {@link #getSelectedValue()} is usually what you want
   */
  @Override
  public final T getSelectedItem() {
    if (selectedItem == null && nullValue != null) {
      return nullValue;
    }

    return selectedItem;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedItem(final Object anItem) {
    T toSelect = translateSelectionItem(anItem);
    if (Objects.equals(nullValue, toSelect)) {
      toSelect = null;
    }
    if (allowSelectionChange(toSelect) && !Objects.equals(selectedItem, toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionChangedEvent.fire(selectedItem);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setFilterSelectedItem(final boolean value) {
    this.filterSelectedItem = value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFilterSelectedItem() {
    return filterSelectedItem;
  }

  /** {@inheritDoc} */
  @Override
  public final void addListDataListener(final ListDataListener listener) {
    Objects.requireNonNull(listener, "listener");
    listDataListeners.add(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeListDataListener(final ListDataListener listener) {
    Objects.requireNonNull(listener, "listener");
    listDataListeners.remove(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final T getElementAt(final int index) {
    final T element = visibleItems.get(index);
    if (element == null) {
      return nullValue;
    }

    return element;
  }

  /** {@inheritDoc} */
  @Override
  public final int getSize() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  @Override
  public final void addFilteringListener(final EventListener listener) {
    filteringDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeFilteringListener(final EventListener listener) {
    filteringDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectionListener(final EventInfoListener<T> listener) {
    selectionChangedEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSelectionListener(final EventInfoListener listener) {
    selectionChangedEvent.removeInfoListener(listener);
  }

  /**
   * @return a List containing the items to be shown in this combo box model,
   * by default it simply returns a list containing the items currently contained in the model,
   * both filtered and visible, excluding the null value.
   */
  protected List<T> initializeContents() {
    final List<T> contents = new ArrayList<>(visibleItems);
    if (nullValue != null) {
      contents.remove(null);
    }
    contents.addAll(filteredItems);

    return contents;
  }

  /**
   * @param itemToSelect the item to be selected
   * @return true if the selection change is ok, false if it should be vetoed
   */
  protected boolean allowSelectionChange(final T itemToSelect) {
    return true;
  }

  /**
   * @param item the item to be selected
   * @return the actual item to select
   */
  protected T translateSelectionItem(final Object item) {
    return (T) item;
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

  /**
   * Sorts the items visible in this model
   */
  private void sortVisibleItems() {
    if (sortComparator != null) {
      visibleItems.sort(sortComparator);
      fireContentsChanged();
    }
  }

  private static final class SortComparator<T> implements Comparator<T>, Serializable {
    private static final long serialVersionUID = 1;

    private final T nullValue;
    private final Comparator comparator = TextUtil.getSpaceAwareCollator();

    SortComparator(final T nullValue) {
      this.nullValue = nullValue;
    }

    @Override
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
      if (nullValue != null) {
        if (o1.equals(nullValue)) {
          return -1;
        }
        if (o2.equals(nullValue)) {
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