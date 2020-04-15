/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Text;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.model.combobox.FilteredComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilteredComboBoxModel} implementation.
 * @param <T> the type of values in this combo box model
 */
public class SwingFilteredComboBoxModel<T> implements FilteredComboBoxModel<T>, ComboBoxModel<T> {

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
  private Predicate<T> includeCondition;
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

  @Override
  public final void refresh() {
    setContents(initializeContents());
  }

  @Override
  public final void clear() {
    setSelectedItem(null);
    setContents(null);
  }

  @Override
  public final boolean isCleared() {
    return cleared;
  }

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

  @Override
  public final void filterContents() {
    try {
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      if (includeCondition != null) {
        for (final Iterator<T> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
          final T item = iterator.next();
          if (item != null && !includeCondition.test(item)) {
            filteredItems.add(item);
            iterator.remove();
          }
        }
      }
      sortVisibleItems();
      if (selectedItem != null && visibleItems.contains(selectedItem)) {
        //update the selected item since the underlying data could have changed
        selectedItem = visibleItems.get(visibleItems.indexOf(selectedItem));
      }
      if (selectedItem != null && !visibleItems.contains(selectedItem) && filterSelectedItem) {
        setSelectedItem(null);
      }
      else {
        fireContentsChanged();
      }
    }
    finally {
      filteringDoneEvent.onEvent();
    }
  }

  @Override
  public final List<T> getVisibleItems() {
    if (visibleItems.isEmpty()) {
      return emptyList();
    }
    if (nullValue == null) {
      return unmodifiableList(visibleItems);
    }

    return unmodifiableList(visibleItems.subList(1, getSize()));
  }

  @Override
  public final List<T> getFilteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final List<T> getItems() {
    final List<T> entities = new ArrayList<>(getVisibleItems());
    entities.addAll(filteredItems);

    return entities;
  }

  @Override
  public final void setIncludeCondition(final Predicate<T> includeCondition) {
    this.includeCondition = includeCondition;
    filterContents();
  }

  @Override
  public final Predicate<T> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean isVisible(final T item) {
    if (item == null) {
      return nullValue != null;
    }

    return visibleItems.contains(item);
  }

  @Override
  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  @Override
  public final void addItem(final T item) {
    if (includeCondition == null || includeCondition.test(item)) {
      if (!visibleItems.contains(item)) {
        visibleItems.add(item);
        sortVisibleItems();
      }
    }
    else if (!filteredItems.contains(item)) {
      filteredItems.add(item);
    }
  }

  @Override
  public final void removeItem(final T item) {
    visibleItems.remove(item);
    filteredItems.remove(item);
    fireContentsChanged();
  }

  @Override
  public final void replaceItem(final T item, final T replacement) {
    removeItem(item);
    addItem(replacement);
    if (Objects.equals(selectedItem, item)) {
      selectedItem = translateSelectionItem(null);
      setSelectedItem(replacement);
    }
  }

  @Override
  public final boolean containsItem(final T item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  @Override
  public final Comparator<T> getSortComparator() {
    return sortComparator;
  }

  @Override
  public final void setSortComparator(final Comparator<T> sortComparator) {
    this.sortComparator = sortComparator;
    sortVisibleItems();
  }

  @Override
  public final T getNullValue() {
    return nullValue;
  }

  @Override
  public final void setNullValue(final T nullValue) {
    this.nullValue = nullValue;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  @Override
  public final boolean isNullValueSelected() {
    return selectedItem == null && nullValue != null;
  }

  @Override
  public final boolean isSelectionEmpty() {
    return getSelectedValue() == null;
  }

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

  @Override
  public final void setSelectedItem(final Object anItem) {
    T toSelect = translateSelectionItem(anItem);
    if (Objects.equals(nullValue, toSelect)) {
      toSelect = null;
    }
    if (!Objects.equals(selectedItem, toSelect) && allowSelectionChange(toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionChangedEvent.onEvent(selectedItem);
    }
  }

  @Override
  public void setFilterSelectedItem(final boolean filterSelectedItem) {
    this.filterSelectedItem = filterSelectedItem;
  }

  @Override
  public boolean isFilterSelectedItem() {
    return filterSelectedItem;
  }

  @Override
  public final void addListDataListener(final ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.add(listener);
  }

  @Override
  public final void removeListDataListener(final ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.remove(listener);
  }

  @Override
  public final T getElementAt(final int index) {
    final T element = visibleItems.get(index);
    if (element == null) {
      return nullValue;
    }

    return element;
  }

  @Override
  public final int getSize() {
    return visibleItems.size();
  }

  @Override
  public final void addFilteringListener(final EventListener listener) {
    filteringDoneEvent.addListener(listener);
  }

  @Override
  public final void removeFilteringListener(final EventListener listener) {
    filteringDoneEvent.removeListener(listener);
  }

  @Override
  public final void addSelectionListener(final EventDataListener<T> listener) {
    selectionChangedEvent.addDataListener(listener);
  }

  @Override
  public final void removeSelectionListener(final EventDataListener<T> listener) {
    selectionChangedEvent.removeDataListener(listener);
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
    if (sortComparator != null && !visibleItems.isEmpty()) {
      visibleItems.sort(sortComparator);
      fireContentsChanged();
    }
  }

  private static final class SortComparator<T> implements Comparator<T> {

    private final T nullValue;
    private final Comparator comparator = Text.getSpaceAwareCollator();

    SortComparator(final T nullValue) {
      this.nullValue = nullValue;
    }

    @Override
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