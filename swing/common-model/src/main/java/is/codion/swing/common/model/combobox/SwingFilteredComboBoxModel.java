/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.combobox;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.worker.ProgressWorker;

import javax.swing.ComboBoxModel;
import javax.swing.SwingUtilities;
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

  private final Event<T> selectionChangedEvent = Event.event();
  private final Event<?> filterEvent = Event.event();
  private final Event<?> refreshEvent = Event.event();
  private final Event<Throwable> refreshFailedEvent = Event.event();
  private final State refreshingState = State.state();

  private final List<T> visibleItems = new ArrayList<>();
  private final List<T> filteredItems = new ArrayList<>();

  /**
   * set during setContents
   */
  private boolean cleared = true;

  private Comparator<T> sortComparator;
  private T selectedItem = null;
  private String nullString;
  private Predicate<T> includeCondition;
  private boolean filterSelectedItem = true;
  private boolean asyncRefresh = FilteredModel.ASYNC_REFRESH.get();

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
   * @param nullString a String representing a null value, which is shown at the top of the item list
   */
  public SwingFilteredComboBoxModel(final String nullString) {
    this(nullString, new SortComparator<>(nullString));
  }

  /**
   * Instantiates a new SwingFilteredComboBoxModel with the given nullValueString.
   * @param nullString a value representing a null value, which is shown at the top of the item list
   * @param sortComparator the Comparator used to sort the contents of this combo box model, if null then
   * the contents are not sorted
   * @see #isNullValueSelected()
   */
  public SwingFilteredComboBoxModel(final String nullString, final Comparator<T> sortComparator) {
    this.nullString = nullString;
    this.sortComparator = sortComparator;
  }

  @Override
  public final void refresh() {
    if (asyncRefresh && SwingUtilities.isEventDispatchThread()) {
      refreshAsync();
    }
    else {
      refreshSync();
    }
  }

  @Override
  public final StateObserver getRefreshingObserver() {
    return refreshingState.getObserver();
  }

  @Override
  public final boolean isAsyncRefresh() {
    return asyncRefresh;
  }

  @Override
  public final void setAsyncRefresh(final boolean asyncRefresh) {
    this.asyncRefresh = asyncRefresh;
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
      if (nullString != null) {
        visibleItems.add(0, null);
      }
    }
    filterContents();
    cleared = contents == null;
  }

  @Override
  public final void filterContents() {
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
    filterEvent.onEvent();
  }

  @Override
  public final List<T> getVisibleItems() {
    if (visibleItems.isEmpty()) {
      return emptyList();
    }
    if (nullString == null) {
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
      return nullString != null;
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
  public final String getNullString() {
    return nullString;
  }

  @Override
  public final void setNullString(final String nullString) {
    this.nullString = nullString;
    if (selectedItem == null) {
      setSelectedItem(null);
    }
  }

  @Override
  public final boolean isNullValueSelected() {
    return selectedItem == null && nullString != null;
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
  public final Object getSelectedItem() {
    if (selectedItem == null && nullString != null) {
      return nullString;
    }

    return selectedItem;
  }

  @Override
  public final void setSelectedItem(final Object anItem) {
    final T toSelect = translateSelectionItem(Objects.equals(nullString, anItem) ? null : anItem);
    if (!Objects.equals(selectedItem, toSelect) && allowSelectionChange(toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionChangedEvent.onEvent(selectedItem);
    }
  }

  @Override
  public final void setFilterSelectedItem(final boolean filterSelectedItem) {
    this.filterSelectedItem = filterSelectedItem;
  }

  @Override
  public final boolean isFilterSelectedItem() {
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
      return (T) nullString;//very hacky, buggy even?
    }

    return element;
  }

  @Override
  public final int getSize() {
    return visibleItems.size();
  }

  @Override
  public final void addFilterListener(final EventListener listener) {
    filterEvent.addListener(listener);
  }

  @Override
  public final void removeFilterListener(final EventListener listener) {
    filterEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(final EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshFailedListener(final EventDataListener<Throwable> listener) {
    refreshFailedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRefreshFailedListener(final EventDataListener<Throwable> listener) {
    refreshFailedEvent.removeDataListener(listener);
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
   * @return a Collection containing the items to be shown in this combo box model,
   * by default this simply returns the items currently contained in the model,
   * both filtered and visible, excluding the null value.
   */
  protected Collection<T> refreshItems() {
    final List<T> contents = new ArrayList<>(visibleItems);
    if (nullString != null) {
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

  private void refreshAsync() {
    ProgressWorker.builder(this::refreshItems)
            .onStarted(this::onRefreshStarted)
            .onResult(this::onRefreshResult)
            .onException(this::onRefreshFailed)
            .execute();
  }

  private void refreshSync() {
    onRefreshStarted();
    try {
      onRefreshResult(refreshItems());
    }
    catch (final Exception e) {
      onRefreshFailed(e);
    }
  }

  private void onRefreshStarted() {
    refreshingState.set(true);
  }

  private void onRefreshFailed(final Throwable throwable) {
    refreshingState.set(false);
    refreshFailedEvent.onEvent(throwable);
  }

  private void onRefreshResult(final Collection<T> items) {
    refreshingState.set(false);
    setContents(items);
    refreshEvent.onEvent();
  }

  private static final class SortComparator<T> implements Comparator<T> {

    private final String nullValue;
    private final Comparator<T> comparator = Text.getSpaceAwareCollator();

    SortComparator(final String nullValue) {
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
        if (nullValue.equals(o1.toString())) {
          return -1;
        }
        if (nullValue.equals(o2.toString())) {
          return 1;
        }
      }
      if (o1 instanceof Comparable && o2 instanceof Comparable) {
        return ((Comparable<T>) o1).compareTo(o2);
      }
      else {
        return comparator.compare(o1, o2);
      }
    }
  }
}