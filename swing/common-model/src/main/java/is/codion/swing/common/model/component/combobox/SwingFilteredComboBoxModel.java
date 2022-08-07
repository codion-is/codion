/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
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
  private boolean includeNull;
  private T nullItem;
  private Predicate<T> includeCondition;
  private boolean filterSelectedItem = true;

  /**
   * Due to a java.util.ConcurrentModificationException in OSX
   */
  private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

  /**
   * Instantiates a new SwingFilteredComboBoxModel.
   * The model contents are sorted automatically with a default collation based comparator.
   */
  public SwingFilteredComboBoxModel() {
    this.sortComparator = new SortComparator<>();
  }

  /**
   * Instantiates a new SwingFilteredComboBoxModel with the given nullValueString.
   * @param sortComparator the Comparator used to sort the contents of this combo box model, if null then
   * the contents are not sorted
   * @see #isNullSelected()
   */
  public SwingFilteredComboBoxModel(Comparator<T> sortComparator) {
    this.sortComparator = sortComparator;
  }

  @Override
  public final void refresh() {
    if (SwingUtilities.isEventDispatchThread()) {
      refreshAsync();
    }
    else {
      refreshSync();
    }
  }

  @Override
  public final StateObserver refreshingObserver() {
    return refreshingState.observer();
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
  public final void setContents(Collection<T> contents) {
    filteredItems.clear();
    visibleItems.clear();
    if (contents != null) {
      visibleItems.addAll(contents);
      if (includeNull) {
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
      for (Iterator<T> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
        T item = iterator.next();
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
  public final List<T> visibleItems() {
    if (visibleItems.isEmpty()) {
      return emptyList();
    }
    if (!includeNull) {
      return unmodifiableList(visibleItems);
    }

    return unmodifiableList(visibleItems.subList(1, getSize()));
  }

  @Override
  public final List<T> filteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final List<T> items() {
    List<T> entities = new ArrayList<>(visibleItems());
    entities.addAll(filteredItems);

    return entities;
  }

  @Override
  public final void setIncludeCondition(Predicate<T> includeCondition) {
    this.includeCondition = includeCondition;
    filterContents();
  }

  @Override
  public final Predicate<T> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final int filteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public final int visibleItemCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean isVisible(T item) {
    if (item == null) {
      return includeNull;
    }

    return visibleItems.contains(item);
  }

  @Override
  public final boolean isFiltered(T item) {
    return filteredItems.contains(item);
  }

  @Override
  public final void addItem(T item) {
    requireNonNull(item);
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
  public final void removeItem(T item) {
    requireNonNull(item);
    visibleItems.remove(item);
    filteredItems.remove(item);
    fireContentsChanged();
  }

  @Override
  public final void replaceItem(T item, T replacement) {
    removeItem(item);
    addItem(replacement);
    if (Objects.equals(selectedItem, item)) {
      selectedItem = translateSelectionItem(null);
      setSelectedItem(replacement);
    }
  }

  @Override
  public final boolean containsItem(T item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  @Override
  public final Comparator<T> getSortComparator() {
    return sortComparator;
  }

  @Override
  public final void setSortComparator(Comparator<T> sortComparator) {
    this.sortComparator = sortComparator;
    sortVisibleItems();
  }

  @Override
  public final void setIncludeNull(boolean includeNull) {
    this.includeNull = includeNull;
    if (includeNull) {
      visibleItems.add(0, null);
    }
    else {
      visibleItems.remove(null);
    }
  }

  @Override
  public final boolean isIncludeNull() {
    return includeNull;
  }

  @Override
  public final void setNullItem(T nullItem) {
    this.nullItem = nullItem;
  }

  @Override
  public final boolean isNullSelected() {
    return includeNull && selectedItem == null;
  }

  @Override
  public final boolean isSelectionEmpty() {
    return selectedValue() == null;
  }

  @Override
  public final T selectedValue() {
    if (isNullSelected()) {
      return null;
    }

    return selectedItem;
  }

  /**
   * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
   * via {@link #setNullItem(Object)}, {@link #selectedValue()} is usually what you want
   */
  @Override
  public final T getSelectedItem() {
    if (selectedItem == null && nullItem != null) {
      return nullItem;
    }

    return selectedItem;
  }

  @Override
  public final void setSelectedItem(Object anItem) {
    T toSelect = translateSelectionItem(Objects.equals(nullItem, anItem) ? null : anItem);
    if (!Objects.equals(selectedItem, toSelect) && allowSelectionChange(toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionChangedEvent.onEvent(selectedItem);
    }
  }

  @Override
  public final void setFilterSelectedItem(boolean filterSelectedItem) {
    this.filterSelectedItem = filterSelectedItem;
  }

  @Override
  public final boolean isFilterSelectedItem() {
    return filterSelectedItem;
  }

  @Override
  public final void addListDataListener(ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.add(listener);
  }

  @Override
  public final void removeListDataListener(ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.remove(listener);
  }

  @Override
  public final T getElementAt(int index) {
    T element = visibleItems.get(index);
    if (element == null) {
      return nullItem;
    }

    return element;
  }

  @Override
  public final int getSize() {
    return visibleItems.size();
  }

  @Override
  public final <V> Value<V> createSelectorValue(Finder<T, V> finder) {
    return new SelectorValue<>(finder);
  }

  @Override
  public final void addFilterListener(EventListener listener) {
    filterEvent.addListener(listener);
  }

  @Override
  public final void removeFilterListener(EventListener listener) {
    filterEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshListener(EventListener listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRefreshFailedListener(EventDataListener<Throwable> listener) {
    refreshFailedEvent.removeDataListener(listener);
  }

  @Override
  public final void addSelectionListener(EventDataListener<T> listener) {
    selectionChangedEvent.addDataListener(listener);
  }

  @Override
  public final void removeSelectionListener(EventDataListener<T> listener) {
    selectionChangedEvent.removeDataListener(listener);
  }

  /**
   * @return a Collection containing the items to be shown in this combo box model,
   * by default this simply returns the items currently contained in the model,
   * both filtered and visible, excluding the null value.
   */
  protected Collection<T> refreshItems() {
    List<T> contents = new ArrayList<>(visibleItems);
    if (includeNull) {
      contents.remove(null);
    }
    contents.addAll(filteredItems);

    return contents;
  }

  /**
   * @param itemToSelect the item to be selected
   * @return true if the selection change is ok, false if it should be vetoed
   */
  protected boolean allowSelectionChange(T itemToSelect) {
    return true;
  }

  /**
   * @param item the item to be selected
   * @return the actual item to select
   */
  protected T translateSelectionItem(Object item) {
    return (T) item;
  }

  /**
   * Fires a {@link ListDataEvent#CONTENTS_CHANGED} event on all registered listeners
   */
  protected final void fireContentsChanged() {
    ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (ListDataListener dataListener : listDataListeners) {
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
            .onException(this::onRefreshFailedAsync)
            .execute();
  }

  private void refreshSync() {
    onRefreshStarted();
    try {
      onRefreshResult(refreshItems());
    }
    catch (Exception e) {
      onRefreshFailedSync(e);
    }
  }

  private void onRefreshStarted() {
    refreshingState.set(true);
  }

  private void onRefreshFailedAsync(Throwable throwable) {
    refreshingState.set(false);
    refreshFailedEvent.onEvent(throwable);
  }

  private void onRefreshFailedSync(Throwable throwable) {
    refreshingState.set(false);
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }

    throw new RuntimeException(throwable);
  }

  private void onRefreshResult(Collection<T> items) {
    refreshingState.set(false);
    setContents(items);
    refreshEvent.onEvent();
  }

  private static final class SortComparator<T> implements Comparator<T> {

    private final Comparator<T> comparator = Text.spaceAwareCollator();

    @Override
    public int compare(T o1, T o2) {
      if (o1 == null && o2 == null) {
        return 0;
      }
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      if (o1 instanceof Comparable && o2 instanceof Comparable) {
        return ((Comparable<T>) o1).compareTo(o2);
      }
      else {
        return comparator.compare(o1, o2);
      }
    }
  }

  private final class SelectorValue<V> extends AbstractValue<V> {

    private final Finder<T, V> finder;

    private SelectorValue(Finder<T, V> finder) {
      this.finder = requireNonNull(finder);
      addSelectionListener(selected -> notifyValueChange());
    }

    @Override
    public V get() {
      if (isSelectionEmpty()) {
        return null;
      }

      return finder.value(selectedValue());
    }

    @Override
    protected void setValue(V value) {
      setSelectedItem(value == null ? null : finder.findItem(visibleItems(), value));
    }
  }
}