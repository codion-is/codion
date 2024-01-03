/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

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
   * @see Refresher#asyncRefresh()
   */
  PropertyValue<Boolean> ASYNC_REFRESH = Configuration.booleanValue("is.codion.common.model.FilteredModel.asyncRefresh", true);

  /**
   * Filters this model according to the condition specified by {@link #includeCondition()}.
   * If no include condition is specified this method does nothing.
   * This method does not interfere with the internal ordering of the visible items.
   * @see #includeCondition()
   */
  void filterItems();

  /**
   * @return the include condition value
   */
  Value<Predicate<T>> includeCondition();

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
  int visibleCount();

  /**
   * @return the number of currently filtered items
   */
  int filteredCount();

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
  boolean visible(T item);

  /**
   * Returns true if this model contains the given item, and it is filtered, that is, is not visible
   * @param item the item
   * @return true if the given item is filtered
   */
  boolean filtered(T item);

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
   * Use {@link Refresher#addRefreshFailedListener(Consumer)} to listen for exceptions that happen during asynchronous refresh.
   * @param afterRefresh called after a successful refresh, may be null
   * @see Refresher#observer()
   * @see Refresher#addRefreshListener(Runnable)
   * @see Refresher#addRefreshFailedListener(Consumer)
   * @see Refresher#asyncRefresh()
   */
  void refreshThen(Consumer<Collection<T>> afterRefresh);

  /**
   * Handles refreshing data for a {@link FilteredModel}.
   * @param <T> the row type
   */
  interface Refresher<T> {

    /**
     * Sometimes we'd like to be able to refresh one or more models and perform some action on
     * the refreshed data, after the refresh has finished, such as selecting a particular item or such.
     * This is quite difficult to achieve with asynchronous refresh enabled, so here's a way to temporarily
     * disable asynchronous refresh, for a more predictable behaviour.
     * @return the State controlling whether asynchronous refreshing should be enabled, true by default
     * @see #ASYNC_REFRESH
     */
    State asyncRefresh();

    /**
     * @return a Value controlling the item supplier for this refresher instance
     */
    Value<Supplier<Collection<T>>> itemSupplier();

    /**
     * Refreshes the items in the associated filtered model.
     * Note that this method only throws exceptions when run synchronously.
     * @throws RuntimeException in case of an exception when running synchronously.
     * @see #addRefreshFailedListener(Consumer)
     * @see #asyncRefresh()
     */
    void refresh();

    /**
     * Refreshes the data in this model. Note that this method only throws exceptions when run synchronously.
     * Use {@link #addRefreshFailedListener(Consumer)} to listen for exceptions that happen during asynchronous refresh.
     * @param afterRefresh called after a successful refresh, may be null
     * @throws RuntimeException in case of an exception when running synchronously.
     * @see #observer()
     * @see #addRefreshListener(Runnable)
     * @see #addRefreshFailedListener(Consumer)
     * @see #asyncRefresh()
     */
    void refreshThen(Consumer<Collection<T>> afterRefresh);

    /**
     * @return an observer active while a refresh is in progress
     */
    StateObserver observer();

    /**
     * @param listener a listener to be notified each time this model has been successfully refreshed
     * @see #refresh()
     */
    void addRefreshListener(Runnable listener);

    /**
     * @param listener the listener to remove
     * @see #refresh()
     */
    void removeRefreshListener(Runnable listener);

    /**
     * @param listener a listener to be notified each time an asynchronous refresh has failed
     * @see #refresh()
     */
    void addRefreshFailedListener(Consumer<Throwable> listener);

    /**
     * @param listener the listener to remove
     */
    void removeRefreshFailedListener(Consumer<Throwable> listener);
  }

  /**
   * An abstract base implementation of {@link Refresher}.
   * @param <T> the model item type
   */
  abstract class AbstractRefresher<T> implements Refresher<T> {

    private final Event<?> refreshEvent = Event.event();
    private final Event<Throwable> refreshFailedEvent = Event.event();
    private final State refreshingState = State.state();
    private final Value<Supplier<Collection<T>>> itemSupplier;
    private final State asyncRefresh = State.state(ASYNC_REFRESH.get());

    /**
     * @param itemSupplier the item supplier
     */
    protected AbstractRefresher(Supplier<Collection<T>> itemSupplier) {
      this.itemSupplier = Value.value(requireNonNull(itemSupplier), itemSupplier);
    }

    @Override
    public final State asyncRefresh() {
      return asyncRefresh;
    }

    @Override
    public final Value<Supplier<Collection<T>>> itemSupplier() {
      return itemSupplier;
    }

    @Override
    public final void refresh() {
      refreshThen(null);
    }

    @Override
    public final void refreshThen(Consumer<Collection<T>> afterRefresh) {
      if (asyncRefresh.get() && supportsAsyncRefresh()) {
        refreshAsync(afterRefresh);
      }
      else {
        refreshSync(afterRefresh);
      }
    }

    @Override
    public final StateObserver observer() {
      return refreshingState.observer();
    }

    @Override
    public final void addRefreshListener(Runnable listener) {
      refreshEvent.addListener(listener);
    }

    @Override
    public final void removeRefreshListener(Runnable listener) {
      refreshEvent.removeListener(listener);
    }

    @Override
    public final void addRefreshFailedListener(Consumer<Throwable> listener) {
      refreshFailedEvent.addDataListener(listener);
    }

    @Override
    public final void removeRefreshFailedListener(Consumer<Throwable> listener) {
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
     * Triggers the successful refresh event
     * @see #addRefreshListener(Runnable)
     */
    protected final void refreshEvent() {
      refreshEvent.run();
    }

    /**
     * Triggers the refresh failed event
     * @param throwable the refresh exception
     * @see #addRefreshFailedListener(Consumer)
     */
    protected final void refreshFailedEvent(Throwable throwable) {
      refreshFailedEvent.accept(throwable);
    }

    /**
     * @return true if we're running on a thread which supports async refresh, such as a UI or application thread
     */
    protected abstract boolean supportsAsyncRefresh();

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
