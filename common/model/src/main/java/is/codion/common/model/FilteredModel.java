/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Specifies a data model that can be filtered to hide some or all of the items it contains.
 * @param <T> the type of data in the model.
 */
public interface FilteredModel<T> {

  /**
   * Specifies whether data models should refresh data asynchronously or on the EDT.<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see FilteredModel#setAsyncRefresh(boolean)
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
   * @return an unmodifiable view of all visible and filtered items in this model, in no particular order
   * @see #visibleItems()
   * @see #filteredItems()
   */
  List<T> items();

  /**
   * @return an unmodifiable view of the visible items
   */
  List<T> visibleItems();

  /**
   * @return an unmodifiable view of the filtered items
   */
  List<T> filteredItems();

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
   * Refreshes the data in this model. Note that this method only throws exceptions when run synchronously off the EDT.
   * Use {@link #addRefreshFailedListener(EventDataListener)} to listen for exceptions that happen during asynchronous refresh.
   * @see #refreshingObserver()
   * @see #addRefreshListener(EventListener)
   * @see #addRefreshFailedListener(EventDataListener)
   * @see #setAsyncRefresh(boolean)
   */
  void refresh();

  /**
   * Refreshes the data in this model. Note that this method only throws exceptions when run synchronously off the EDT.
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
