/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a data model that can be filtered to hide some or all of the items it contains.
 * @param <T> the type of data in the model.
 */
public interface FilteredModel<T> {

  /**
   * Specifies whether data models should refresh data asynchronously or on the EDT.<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see FilteredModel.Refresher#setAsyncRefresh(boolean)
   */
  PropertyValue<Boolean> ASYNC_REFRESH = Configuration.booleanValue("is.codion.common.model.FilteredModel.asyncRefresh", true);

  /**
   * Filters this model according to the condition returned by {@link #getIncludeCondition()}.
   * If no include condition is specified this method does nothing.
   * This method does not interfere with the internal ordering of the visible items.
   * @see #getIncludeCondition()
   * @see #setIncludeCondition(Predicate)
   */
  void filterItems();

  /**
   * Returns the include condition used by this model, null if no include condition has been set.
   * @return the include condition
   */
  Predicate<T> getIncludeCondition();

  /**
   * Sets the include condition and filters the model
   * @param includeCondition the Predicate to use when filtering, null if no filtering should be performed
   */
  void setIncludeCondition(Predicate<T> includeCondition);

  /**
   * @return an unmodifiable view of all visible and filtered items in this model
   * @see #visibleItems()
   * @see #filteredItems()
   */
  Collection<T> items();

  /**
   * @return an unmodifiable view of the visible items, in the order they appear in the model
   */
  List<T> visibleItems();

  /**
   * @return an unmodifiable view of the filtered items
   */
  Collection<T> filteredItems();

  /**
   * @return the number of currently visible items
   */
  int visibleItemCount();

  /**
   * @return the number of currently filtered items
   */
  int filteredItemCount();

  /**
   * Returns true if this model contains the given item, visible or filtered.
   * @param item the item
   * @return true if this model contains the item
   */
  boolean containsItem(T item);

  /**
   * Returns true if this model contains the given item, and it is visible, that is, not filtered
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(T item);

  /**
   * Returns true if this model contains the given item, and it is filtered, that is, is not visible
   * @param item the item
   * @return true if the given item is filtered
   */
  boolean isFiltered(T item);

  /**
   * @return this models Refresher instance
   */
  Refresher<T> refresher();

  /**
   * Refreshes the items in this filtered model using its {@link Refresher}.
   * @throws RuntimeException in case of an exception when running refresh synchronously, as in, not on the user interface thread
   * @see Refresher#refresh()
   */
  void refresh();

  /**
   * Refreshes the data in this filtered model using its {@link Refresher}.
   * Note that this method only throws exceptions when run synchronously off the user interface thread.
   * Use {@link Refresher#addRefreshFailedListener(EventDataListener)} to listen for exceptions that happen during asynchronous refresh.
   * @param afterRefresh called after a successful refresh, may be null
   * @see Refresher#refreshingObserver()
   * @see Refresher#addRefreshListener(EventListener)
   * @see Refresher#addRefreshFailedListener(EventDataListener)
   * @see Refresher#setAsyncRefresh(boolean)
   */
  void refreshThen(Consumer<Collection<T>> afterRefresh);

  /**
   * Handles refreshing data for a {@link FilteredModel}.
   * @param <T> the row type
   */
  interface Refresher<T> {

    /**
     * @return true if asynchronous refreshing is enabled, true by default
     * @see #ASYNC_REFRESH
     */
    boolean isAsyncRefresh();

    /**
     * Sometimes we'd like to be able to refresh one or more models and perform some action on
     * the refreshed data, after the refresh has finished, such as selecting a particular item or such.
     * This is quite difficult to achieve with asynchronous refresh enabled, so here's a way to temporarily
     * disable asynchronous refresh, for a more predictable behaviour.
     * @param asyncRefresh true if asynchronous refreshing should be enabled, true by default
     * @see #ASYNC_REFRESH
     */
    void setAsyncRefresh(boolean asyncRefresh);

    /**
     * @return the item supplier
     */
    Supplier<Collection<T>> getItemSupplier();

    /**
     * Supplies the items when {@link #refresh()} is called.
     * @param itemSupplier the item supplier
     */
    void setItemSupplier(Supplier<Collection<T>> itemSupplier);

    /**
     * Refreshes the items in the associated filtered model.
     * If run on the Event Dispatch Thread and {@link #isAsyncRefresh()} returns true, the refresh happens asynchronously.
     * @throws RuntimeException in case of an exception when running refresh synchronously, as in, not on the user interface thread.
     * @see #addRefreshFailedListener(EventDataListener)
     * @see #setAsyncRefresh(boolean)
     */
    void refresh();

    /**
     * Refreshes the data in this model. Note that this method only throws exceptions when run synchronously off the user interface thread.
     * Use {@link #addRefreshFailedListener(EventDataListener)} to listen for exceptions that happen during asynchronous refresh.
     * @param afterRefresh called after a successful refresh, may be null
     * @see #refreshingObserver()
     * @see #addRefreshListener(EventListener)
     * @see #addRefreshFailedListener(EventDataListener)
     * @see #setAsyncRefresh(boolean)
     */
    void refreshThen(Consumer<Collection<T>> afterRefresh);

    /**
     * @return an observer active while a refresh is in progress
     */
    StateObserver refreshingObserver();

    /**
     * @param listener a listener to be notified each time this model has been successfully refreshed
     * @see #refresh()
     */
    void addRefreshListener(EventListener listener);

    /**
     * @param listener the listener to remove
     * @see #refresh()
     */
    void removeRefreshListener(EventListener listener);

    /**
     * @param listener a listener to be notified each time an asynchronous refresh has failed
     * @see #refresh()
     */
    void addRefreshFailedListener(EventDataListener<Throwable> listener);

    /**
     * @param listener the listener to remove
     */
    void removeRefreshFailedListener(EventDataListener<Throwable> listener);
  }

  /**
   * An abstract base implementation of {@link Refresher}.
   * @param <T> the model item type
   */
  abstract class AbstractRefresher<T> implements Refresher<T> {

    private final Event<?> refreshEvent = Event.event();
    private final Event<Throwable> refreshFailedEvent = Event.event();
    private final State refreshingState = State.state();

    private boolean asyncRefresh = ASYNC_REFRESH.get();

    private Supplier<Collection<T>> itemSupplier;

    /**
     * @param itemSupplier the item supplier
     */
    protected AbstractRefresher(Supplier<Collection<T>> itemSupplier) {
      this.itemSupplier = requireNonNull(itemSupplier);
    }

    @Override
    public final boolean isAsyncRefresh() {
      return asyncRefresh;
    }

    @Override
    public final void setAsyncRefresh(boolean asyncRefresh) {
      this.asyncRefresh = asyncRefresh;
    }

    @Override
    public final Supplier<Collection<T>> getItemSupplier() {
      return itemSupplier;
    }

    @Override
    public final void setItemSupplier(Supplier<Collection<T>> itemSupplier) {
      this.itemSupplier = requireNonNull(itemSupplier);
    }

    @Override
    public final void refresh() {
      refreshThen(null);
    }

    @Override
    public final void refreshThen(Consumer<Collection<T>> afterRefresh) {
      if (asyncRefresh && isUserInterfaceThread()) {
        refreshAsync(afterRefresh);
      }
      else {
        refreshSync(afterRefresh);
      }
    }

    @Override
    public final StateObserver refreshingObserver() {
      return refreshingState.observer();
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

    /**
     * Sets the refreshing (active) state of this refresher
     * @param refreshing true if refresh is starting, false if ended
     */
    protected final void setRefreshing(boolean refreshing) {
      refreshingState.set(refreshing);
    }

    /**
     * Fires the refresh event
     * @see #addRefreshListener(EventListener)
     */
    protected final void fireRefreshEvent() {
      refreshEvent.onEvent();
    }

    /**
     * Fires the refresh failed event
     * @param throwable the refresh exception
     * @see #addRefreshFailedListener(EventDataListener)
     */
    protected final void fireRefreshFailedEvent(Throwable throwable) {
      refreshFailedEvent.onEvent(throwable);
    }

    /**
     * @return true if we're running on the user interface thread (meaning an async refresh is in order)
     */
    protected abstract boolean isUserInterfaceThread();

    /**
     * Performes an async refresh
     * @param afterRefresh if specified will be called after a successful refresh
     */
    protected abstract void refreshAsync(Consumer<Collection<T>> afterRefresh);

    /**
     * Performs a sync refresh
     * @param afterRefresh if specified will be called after a successful refresh
     */
    protected abstract void refreshSync(Consumer<Collection<T>> afterRefresh);

    /**
     * Processes the refresh result, by replacing the current model items by the result items.
     * @param items the items resulting from the refresh operation
     */
    protected abstract void processResult(Collection<T> items);
  }
}
