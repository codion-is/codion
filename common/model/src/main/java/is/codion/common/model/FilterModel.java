/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.model.selection.SingleSelectionModel;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Specifies a data model that can be filtered to hide some or all of the items it contains.
 * @param <T> the type of items in the model.
 */
public interface FilterModel<T> {

	/**
	 * Specifies whether data models should refresh data asynchronously or on the UI thread
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * @see Refresher#async()
	 */
	PropertyValue<Boolean> ASYNC_REFRESH = Configuration.booleanValue(FilterModel.class.getName() + ".asyncRefresh", true);

	/**
	 * @return the model items
	 */
	Items<T> items();

	/**
	 * @return this models Refresher instance
	 */
	Refresher<T> refresher();

	/**
	 * Adds the given item to this model.
	 * Note that if the item does not fulfill the visible predicate, it will be filtered right away.
	 * @param item the item to add
	 */
	void addItem(T item);

	/**
	 * @param index the index
	 * @param item the item to add
	 */
	void addItemAt(int index, T item);

	/**
	 * Adds the given items to the bottom of this table model.
	 * @param items the items to add
	 */
	void addItems(Collection<T> items);

	/**
	 * Adds the given item to the bottom of this table model.
	 * If sorting is enabled this model is sorted after the item has been added.
	 * @param item the item to add
	 */
	void addItemSorted(T item);

	/**
	 * Adds the given items to this table model, non-filtered items are added at the given index.
	 * @param index the index at which to add the items
	 * @param items the items to add
	 */
	void addItemsAt(int index, Collection<T> items);

	/**
	 * Adds the given items to the bottom of this table model.
	 * If sorting is enabled this model is sorted after the items have been added.
	 * @param items the items to add
	 */
	void addItemsSorted(Collection<T> items);

	/**
	 * Adds the given items to this table model, non-filtered items are added at the given index.
	 * If a {@link #comparator()} is specified this model is sorted after the items have been added.
	 * @param index the index at which to add the items
	 * @param items the items to add
	 */
	void addItemsAtSorted(int index, Collection<T> items);

	/**
	 * Sets the item at the given index.
	 * If the item should be filtered calling this method has no effect.
	 * @param index the index
	 * @param item the item
	 * @see Items#visiblePredicate()
	 */
	void setItemAt(int index, T item);

	/**
	 * Removes the given item from this model
	 * @param item the item to remove from the model
	 */
	void removeItem(T item);

	/**
	 * Removes from this table model the visible element whose index is between index
	 * @param index the index of the row to be removed
	 * @return the removed item
	 * @throws IndexOutOfBoundsException in case the indexe is out of bounds
	 */
	T removeItemAt(int index);

	/**
	 * Removes the given items from this table model
	 * @param items the items to remove from the model
	 */
	void removeItems(Collection<T> items);

	/**
	 * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
	 * @param fromIndex index of first row to be removed
	 * @param toIndex index after last row to be removed
	 * @return the removed items
	 * @throws IndexOutOfBoundsException in case the indexes are out of bounds
	 */
	List<T> removeItems(int fromIndex, int toIndex);

	/**
	 * Clears all items from this model
	 */
	void clear();

	/**
	 * Sorts the visible items according to {@link #comparator()}, preserving the selection.
	 * Calling this method when no comparator is specified has no effect.
	 * @see #comparator()
	 */
	void sortItems();

	/**
	 * @return the {@link Value} controlling the comparator to use when sorting
	 */
	Value<Comparator<T>> comparator();

	/**
	 * Refreshes the items in this filtered model using its {@link Refresher}.
	 * @throws RuntimeException in case of an exception when running refresh synchronously, as in, not on the user interface thread
	 * @see Refresher#refresh()
	 */
	void refresh();

	/**
	 * Refreshes the data in this filter model using its {@link Refresher}.
	 * Note that this method only throws exceptions when run synchronously off the user interface thread.
	 * Use {@link Refresher#failure()} to listen for exceptions that happen during asynchronous refresh.
	 * @param afterRefresh called after a successful refresh, may be null
	 * @see Refresher#observer()
	 * @see Refresher#success()
	 * @see Refresher#failure()
	 * @see Refresher#async()
	 */
	void refreshThen(Consumer<Collection<T>> afterRefresh);

	/**
	 * @return the selection model
	 */
	SingleSelectionModel<T> selection();

	/**
	 * A {@link Mutable} controlling the items in a {@link FilterModel}
	 * @param <T> the item type
	 */
	interface Items<T> extends Mutable<Collection<T>> {

		/**
		 * @return the {@link Value} controlling the predicate specifying which items should be visible
		 */
		Value<Predicate<T>> visiblePredicate();

		/**
		 * @return a {@link Visible} providing an unmodifiable view of the visible items, in the order they appear in the model
		 */
		Visible<T> visible();

		/**
		 * @return a {@link Filtered} providing an unmodifiable view of the filtered items
		 */
		Filtered<T> filtered();

		/**
		 * Returns true if these items contain the given item, visible or filtered.
		 * @param item the item
		 * @return true if this model contains the item
		 */
		boolean contains(T item);

		/**
		 * Filters the items according to the condition specified by {@link #visiblePredicate()}.
		 * If no include condition is specified this method does nothing.
		 * This method does not interfere with the internal ordering of the visible items.
		 * @see #visiblePredicate()
		 */
		void filter();

		/**
		 * @param <T> the item type
		 */
		interface Visible<T> extends Observable<List<T>> {

			/**
			 * Returns true if the given item is visible
			 * @param item the item
			 * @return true if the item is visible
			 */
			boolean contains(T item);

			/**
			 * @param item the item
			 * @return the index of the item in this model
			 */
			int indexOf(T item);

			/**
			 * @param index the row index
			 * @return the item at the given index in this model
			 */
			T itemAt(int index);

			/**
			 * @return the number of visible items
			 */
			int size();
		}

		/**
		 * @param <T> the item type
		 */
		interface Filtered<T> extends Observable<Collection<T>> {

			/**
			 * Returns true if the given item is filtered.
			 * @param item the item
			 * @return true if the item is filtered
			 */
			boolean contains(T item);

			/**
			 * @return the number of filtered items
			 */
			int size();
		}
	}

	/**
	 * Handles refreshing data for a {@link FilterModel}.
	 * @param <T> the row type
	 */
	interface Refresher<T> {

		/**
		 * Sometimes we'd like to be able to refresh one or more models and perform some action on
		 * the refreshed data, after the refresh has finished, such as selecting a particular item or such.
		 * This is quite difficult to achieve with asynchronous refresh enabled, so here's a way to temporarily
		 * disable asynchronous refresh, for a more predictable behaviour.
		 * @return the {@link State} controlling whether asynchronous refreshing is enabled
		 * @see #ASYNC_REFRESH
		 */
		State async();

		/**
		 * @return a Value controlling the item supplier for this refresher instance
		 */
		Value<Supplier<Collection<T>>> items();

		/**
		 * Refreshes the items in the associated filter model.
		 * Note that this method only throws exceptions when run synchronously.
		 * @throws RuntimeException in case of an exception when running synchronously.
		 * @see #failure()
		 * @see #async()
		 */
		void refresh();

		/**
		 * Refreshes the data in this model. Note that this method only throws exceptions when run synchronously.
		 * Use {@link #failure()} to listen for exceptions that happen during asynchronous refresh.
		 * @param afterRefresh called after a successful refresh, may be null
		 * @throws RuntimeException in case of an exception when running synchronously.
		 * @see #observer()
		 * @see #success()
		 * @see #failure()
		 * @see #async()
		 */
		void refreshThen(Consumer<Collection<T>> afterRefresh);

		/**
		 * @return an observer active while a refresh is in progress
		 */
		StateObserver observer();

		/**
		 * @return an observer notified each time a successful refresh has been performed
		 * @see #refresh()
		 */
		Observer<Collection<T>> success();

		/**
		 * @return an observer notified each time an asynchronous refresh has failed
		 * @see #refresh()
		 */
		Observer<Exception> failure();
	}

	/**
	 * An abstract base implementation of {@link Refresher}.
	 * @param <T> the model item type
	 */
	abstract class AbstractRefresher<T> implements Refresher<T> {

		private final Event<Collection<T>> refreshEvent = Event.event();
		private final Event<Exception> refreshFailedEvent = Event.event();
		private final State refreshingState = State.state();
		private final Value<Supplier<Collection<T>>> items;
		private final State async = State.state(ASYNC_REFRESH.get());

		/**
		 * @param items supplies the items
		 */
		protected AbstractRefresher(Supplier<Collection<T>> items) {
			this.items = Value.builder()
							.nonNull(items)
							.build();
		}

		@Override
		public final State async() {
			return async;
		}

		@Override
		public final Value<Supplier<Collection<T>>> items() {
			return items;
		}

		@Override
		public final void refresh() {
			refreshThen(null);
		}

		@Override
		public final void refreshThen(Consumer<Collection<T>> afterRefresh) {
			if (async.get() && supportsAsyncRefresh()) {
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
		public final Observer<Collection<T>> success() {
			return refreshEvent.observer();
		}

		@Override
		public final Observer<Exception> failure() {
			return refreshFailedEvent.observer();
		}

		/**
		 * Sets the refreshing (active) state of this refresher
		 * @param refreshing true if refresh is starting, false if ended
		 */
		protected final void setRefreshing(boolean refreshing) {
			refreshingState.set(refreshing);
		}

		/**
		 * Triggers the successful refresh event with the given items
		 * @param items the refresh result
		 * @see #success()
		 */
		protected final void notifySuccess(Collection<T> items) {
			refreshEvent.accept(items);
		}

		/**
		 * Triggers the refresh failed event
		 * @param exception the refresh exception
		 * @see #failure()
		 */
		protected final void notifyFailure(Exception exception) {
			refreshFailedEvent.accept(exception);
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
