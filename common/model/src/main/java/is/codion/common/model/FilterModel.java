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
import is.codion.common.model.selection.SingleItemSelection;
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
	 * Refreshes the items in this model using its {@link Refresher}.
	 * @throws RuntimeException in case of an exception when running refresh synchronously
	 * @see Refresher#refresh()
	 */
	void refresh();

	/**
	 * Refreshes the data in this filter model using its {@link Refresher}.
	 * Note that this method only throws exceptions when run synchronously off the user interface thread.
	 * Use {@link Refresher#failure()} to listen for exceptions that happen during asynchronous refresh.
	 * @param onRefresh called after a successful refresh, may be null
	 * @see Refresher#observer()
	 * @see Refresher#success()
	 * @see Refresher#failure()
	 * @see Refresher#async()
	 */
	void refresh(Consumer<Collection<T>> onRefresh);

	/**
	 * @return the {@link SingleItemSelection} instance used by this filter model
	 */
	SingleItemSelection<T> selection();

	/**
	 * A {@link Mutable} controlling the items in a {@link FilterModel}
	 * @param <T> the item type
	 */
	interface Items<T> extends Mutable<Collection<T>> {

		/**
		 * Adds the given item to this model.
		 * Note that if the item does not pass the {@link #visible()} predicate, it will be filtered right away.
		 * @param item the item to add
		 * @return true if the item was added, false if filtered
		 */
		boolean addItem(T item);

		/**
		 * Adds the given items to the bottom of this table model.
		 * Note that if an item does not pass the {@link #visible()} predicate, it will be filtered right away.
		 * @param items the items to add
		 * @return false if none of the given items passed the {@link #visible()} predicate and were filtered right away
		 */
		boolean addItems(Collection<T> items);

		/**
		 * Removes the given item from this model
		 * @param item the item to remove from the model
		 * @return true if the item was visible
		 */
		boolean removeItem(T item);

		/**
		 * Removes the given items from this table model
		 * @param items the items to remove from the model
		 * @return true if one or more of the items were visible
		 */
		boolean removeItems(Collection<T> items);

		/**
		 * @return a {@link Visible} providing access to the visible items, in the order they appear in the model
		 */
		Visible<T> visible();

		/**
		 * @return a {@link Filtered} providing access to the filtered items
		 */
		Filtered<T> filtered();

		/**
		 * Returns true if the model contain the given item, as visible or filtered.
		 * @param item the item
		 * @return true if this model contains the item
		 */
		boolean contains(T item);

		/**
		 * @return the total number of items, visible and filtered
		 */
		int count();

		/**
		 * Filters the items according to the predicate specified by {@link Visible#predicate()}.
		 * If no visible predicate is specified this method does nothing.
		 * This method does not interfere with the internal ordering of the visible items.
		 * @see Visible#predicate()
		 */
		void filter();

		/**
		 * @param <T> the item type
		 */
		interface Visible<T> extends Observable<List<T>> {

			/**
			 * @return the {@link Value} controlling the predicate specifying which items should be visible
			 */
			Value<Predicate<T>> predicate();

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
			 * Adds the given item at the given index.
			 * Note that if the item does not pass the {@link #visible()} predicate it is filtered right away and the method returns false.
			 * @param index the index
			 * @param item the item to add
			 * @return true if the item was added, false if filtered
			 */
			boolean addItemAt(int index, T item);

			/**
			 * Adds the given items at the last index.
			 * Note that if an item does not pass the {@link #visible()} predicate it is filtered right away.
			 * @param index the index at which to add the items
			 * @param items the items to add
			 * @return false if none of the given items passed the {@link #visible()} predicate and were filtered right away
			 */
			boolean addItemsAt(int index, Collection<T> items);

			/**
			 * Sets the item at the given index.
			 * Note that if the item does not pass the {@link #visible()} predicate it is filtered right away and this method has no effect.
			 * @param index the index
			 * @param item the item
			 * @return true if the item was set, false if it did not pass the {@link #visible()} predicate
			 * @see Items.Visible#predicate()
			 */
			boolean setItemAt(int index, T item);

			/**
			 * Removes from this table model the visible element whose index is between index
			 * @param index the index of the row to be removed
			 * @return the removed item
			 * @throws IndexOutOfBoundsException in case the index is out of bounds
			 */
			T removeItemAt(int index);

			/**
			 * Removes from this table model all visible elements whose index is between fromIndex, inclusive and toIndex, exclusive
			 * @param fromIndex index of first row to be removed
			 * @param toIndex index after last row to be removed
			 * @return the removed items
			 * @throws IndexOutOfBoundsException in case the indexes are out of bounds
			 */
			List<T> removeItems(int fromIndex, int toIndex);

			/**
			 * @return the number of visible items
			 */
			int count();

			/**
			 * Sorts the visible items according to {@link #comparator()}, preserving the selection.
			 * Calling this method when no comparator is specified has no effect.
			 * @see #comparator()
			 */
			void sort();

			/**
			 * @return the {@link Value} controlling the comparator to use when sorting
			 */
			Value<Comparator<T>> comparator();
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
			int count();
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
		 * @return a {@link Value} controlling the item supplier for this refresher instance
		 */
		Value<Supplier<Collection<T>>> supplier();

		/**
		 * @return an observer active while a refresh is in progress
		 */
		StateObserver observer();

		/**
		 * @return an observer notified each time a successful refresh has been performed
		 */
		Observer<Collection<T>> success();

		/**
		 * @return an observer notified each time an asynchronous refresh has failed
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
		private final Value<Supplier<Collection<T>>> supplier;
		private final State async = State.state(ASYNC_REFRESH.get());

		/**
		 * @param supplier supplies the items
		 */
		protected AbstractRefresher(Supplier<Collection<T>> supplier) {
			this.supplier = Value.builder()
							.nonNull(supplier)
							.build();
		}

		@Override
		public final State async() {
			return async;
		}

		@Override
		public final Value<Supplier<Collection<T>>> supplier() {
			return supplier;
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
		 * Refreshes the data. Note that this method only throws exceptions when run synchronously.
		 * Use {@link #failure()} to listen for exceptions that happen during asynchronous refresh.
		 * @param onRefresh called after a successful refresh, may be null
		 * @throws RuntimeException in case of an exception when running synchronously.
		 * @see #observer()
		 * @see #success()
		 * @see #failure()
		 * @see #async()
		 */
		protected final void refresh(Consumer<Collection<T>> onRefresh) {
			if (async.get() && supportsAsyncRefresh()) {
				refreshAsync(onRefresh);
			}
			else {
				refreshSync(onRefresh);
			}
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
		 * @param onRefresh if specified will be called after a successful refresh
		 */
		protected abstract void refreshAsync(Consumer<Collection<T>> onRefresh);

		/**
		 * Performs a sync refresh
		 * @param onRefresh if specified will be called after a successful refresh
		 */
		protected abstract void refreshSync(Consumer<Collection<T>> onRefresh);

		/**
		 * Processes the refresh result, by replacing the current model items by the result items.
		 * @param items the items resulting from the refresh operation
		 */
		protected abstract void processResult(Collection<T> items);
	}
}
