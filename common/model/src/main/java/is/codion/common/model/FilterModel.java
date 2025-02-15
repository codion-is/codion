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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model;

import is.codion.common.event.Event;
import is.codion.common.model.selection.SingleSelection;
import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.Configuration.booleanValue;
import static java.util.Objects.requireNonNull;

/**
 * Specifies a data model that can be filtered to hide some or all of the items it contains.
 * @param <T> the type of items in the model.
 */
public interface FilterModel<T> {

	/**
	 * Specifies whether data models should refresh data asynchronously or on the UI thread
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see Refresher#async()
	 */
	PropertyValue<Boolean> ASYNC_REFRESH = booleanValue(FilterModel.class.getName() + ".asyncRefresh", true);

	/**
	 * @return the model items
	 */
	Items<T> items();

	/**
	 * @return the {@link SingleSelection} instance used by this model
	 */
	SingleSelection<T> selection();

	/**
	 * @return the {@link Sorter} instance used by this model
	 */
	Sorter<T> sorter();

	/**
	 * Manages the items in {@link FilterModel}.
	 * @param <T> the item type
	 */
	interface Items<T> {

		/**
		 * @return this models {@link Refresher} instance
		 */
		Refresher<T> refresher();

		/**
		 * Refreshes the items in this model using its {@link Refresher}.
		 * @throws RuntimeException in case of an exception when running refresh synchronously
		 */
		void refresh();

		/**
		 * <p>Refreshes the data in this model using its {@link Refresher}.
		 * <p>Note that this method only throws exceptions when run synchronously off the user interface thread.
		 * Use {@link Refresher#exception()} to listen for exceptions that happen during asynchronous refresh.
		 * @param onResult called after a successful refresh
		 * @see Refresher#active()
		 * @see Refresher#result()
		 * @see Refresher#exception()
		 * @see Refresher#async()
		 */
		void refresh(Consumer<Collection<T>> onResult);

		/**
		 * @return all items, visible and filtered, in no particular order
		 */
		Collection<T> get();

		/**
		 * It is up to the implementation whether the visible items are sorted when the items are set.
		 * @param items the items
		 */
		void set(Collection<T> items);

		/**
		 * <p>Adds the given item to this model.
		 * <p>If the item passes the {@link VisibleItems#predicate()} it is appended
		 * to the visible items, which are then sorted if sorting is enabled.
		 * <p>If the item does not pass the {@link VisibleItems#predicate()},
		 * it will be filtered right away.
		 * @param item the item to add
		 */
		void add(T item);

		/**
		 * <p>Adds the given items to this model.
		 * <p>Items that pass the {@link VisibleItems#predicate()} are is appended
		 * to the visible items, which are then sorted if sorting is enabled.
		 * <p>If no items pass the {@link VisibleItems#predicate()}, they will
		 * be filtered right away.
		 * @param items the items to add
		 */
		void add(Collection<T> items);

		/**
		 * <p>Removes the given item from this model.
		 * @param item the item to remove from the model
		 */
		void remove(T item);

		/**
		 * <p>Removes the given items from this table model.
		 * @param items the items to remove from the model
		 */
		void remove(Collection<T> items);

		/**
		 * <p>Replaces the first occurrence of the given item. If the item is not found this method has no effect.
		 * <p>Note that this method respects the visible predicate, so a
		 * currently filtered item may be replaced with a visible item and vice verse.
		 * <p>If the visible items change they are sorted if sorting is enabled.
		 * @param item the item to replace
		 * @param replacement the replacement item
		 * @see VisibleItems#predicate()
		 */
		void replace(T item, T replacement);

		/**
		 * Clears the items
		 */
		void clear();

		/**
		 * @return a {@link VisibleItems} providing access to the visible items, in the order they appear in the model
		 */
		VisibleItems<T> visible();

		/**
		 * @return a {@link FilteredItems} providing access to the filtered items
		 */
		FilteredItems<T> filtered();

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
		 * <p>Filters the items according to the {@link VisibleItems#predicate()}.
		 * <p>If no predicate is specified calling this method has no effect.
		 * <p>This method does not interfere with the internal ordering of the visible items.
		 * @see VisibleItems#predicate()
		 */
		void filter();
	}

	/**
	 * @param <T> the item type
	 */
	interface VisibleItems<T> extends Observable<List<T>> {

		/**
		 * @return the visible items or an empty list if no item is visible
		 */
		@Override
		List<T> get();

		/**
		 * @return the {@link Value} controlling the predicate specifying which items should be visible
		 */
		Value<Predicate<T>> predicate();

		/**
		 * @return an {@link Observer} notified when items have been added
		 */
		Observer<Collection<T>> added();

		/**
		 * Returns true if the given item is visible
		 * @param item the item
		 * @return true if the item is visible
		 */
		boolean contains(T item);

		/**
		 * @param item the item
		 * @return the index of the item in this model, -1 if it is not visible
		 */
		int indexOf(T item);

		/**
		 * @param index the row index
		 * @return the item at the given index in this model
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		T get(int index);

		/**
		 * <p>Adds the given item at the given index and sorts the visible items if sorting is enabled.
		 * <p>Note that if the item does not pass the {@link VisibleItems#predicate()} it is filtered right away and the method returns false.
		 * @param index the index
		 * @param item the item to add
		 * @return true if the item was added to the visible items
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		boolean add(int index, T item);

		/**
		 * <p>Adds the given items at the given index and sorts the visible items if sorting is enabled.
		 * <p>Note that if an item does not pass the {@link VisibleItems#predicate()} it is filtered right away.
		 * @param index the index at which to add the items
		 * @param items the items to add
		 * @return true if one or more of the items was added to the visible items
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		boolean add(int index, Collection<T> items);

		/**
		 * <p>Sets the item at the given index.
		 * <p>Note that if the item does not pass the {@link VisibleItems#predicate()} this method has no effect.
		 * @param index the index
		 * @param item the item
		 * @return true if the item was set, false if it did not pass the {@link VisibleItems#predicate()}
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 * @see VisibleItems#predicate()
		 */
		boolean set(int index, T item);

		/**
		 * <p>Removes from this table model the visible element at the given index
		 * @param index the index of the row to be removed
		 * @return the removed item
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		T remove(int index);

		/**
		 * <p>Removes from this table model all visible elements whose index is between {@code fromIndex}, inclusive and {@code toIndex}, exclusive
		 * @param fromIndex index of first row to be removed
		 * @param toIndex index after last row to be removed
		 * @return the removed items
		 * @throws IndexOutOfBoundsException in case the indexes are out of bounds
		 */
		List<T> remove(int fromIndex, int toIndex);

		/**
		 * @return the number of visible items
		 */
		int count();

		/**
		 * Sorts the visible items using {@link Sorter#comparator()}, preserving the selection.
		 * @see FilterModel#sorter()
		 */
		void sortItems();
	}

	/**
	 * @param <T> the item type
	 */
	interface FilteredItems<T> extends Observable<Collection<T>> {

		/**
		 * @return the filtered items or an empty collection in case of no filtered items
		 */
		@Override
		Collection<T> get();

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

	/**
	 * Handles refreshing data for a {@link FilterModel}.
	 * @param <T> the type of items produced by this {@link Refresher}
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
		 * <p>Changes to this state are always triggered on the UI thread.
		 * @return an observable indicating that a refresh is in progress
		 */
		ObservableState active();

		/**
		 * <p>This event is always triggered on the UI thread.
		 * @return an observer notified with the result after a successful refresh
		 */
		Observer<Collection<T>> result();

		/**
		 * <p>This event is always triggered on the UI thread.
		 * @return an observer notified with the exception when an asynchronous refresh has failed
		 */
		Observer<Exception> exception();
	}

	/**
	 * Manages the sorting for a {@link FilterModel}
	 * @param <T> the model item type
	 */
	interface Sorter<T> {

		/**
		 * @return a {@link Comparator} based on this sort model
		 */
		Comparator<T> comparator();

		/**
		 * @return an observer notified each time the sorting changes, the event data indicating whether the model is sorted or not
		 */
		Observer<Boolean> observer();
	}

	/**
	 * An abstract base implementation of {@link Refresher}.
	 * @param <T> the model item type
	 */
	abstract class AbstractRefresher<T> implements Refresher<T> {

		private final Event<Collection<T>> onResult = Event.event();
		private final Event<Exception> onException = Event.event();
		private final State active = State.state();
		private final Supplier<Collection<T>> supplier;
		private final State async = State.state(ASYNC_REFRESH.getOrThrow());

		/**
		 * @param supplier supplies the items when refreshing
		 */
		protected AbstractRefresher(Supplier<Collection<T>> supplier) {
			this.supplier = requireNonNull(supplier);
		}

		@Override
		public final State async() {
			return async;
		}

		@Override
		public final ObservableState active() {
			return active.observable();
		}

		@Override
		public final Observer<Collection<T>> result() {
			return onResult.observer();
		}

		@Override
		public final Observer<Exception> exception() {
			return onException.observer();
		}

		/**
		 * @return the item supplier for this refresher instance
		 */
		protected final Supplier<Collection<T>> supplier() {
			return supplier;
		}

		/**
		 * <p>Refreshes the data. Note that this method only throws exceptions when run synchronously.
		 * <p>Use {@link #exception()} to listen for exceptions that happen during asynchronous refresh.
		 * <p>Async refresh is performed when it is enabled ({@link #async()}) and this method is called on the UI thread.
		 * @param onResult called with the result after a successful refresh, may be null
		 * @throws RuntimeException in case of an exception when running synchronously.
		 * @see #active()
		 * @see #result()
		 * @see #exception()
		 * @see #async()
		 */
		protected final void refresh(@Nullable Consumer<Collection<T>> onResult) {
			if (async.get() && isUserInterfaceThread()) {
				refreshAsync(onResult);
			}
			else {
				refreshSync(onResult);
			}
		}

		/**
		 * <p>Sets the active state of this refresher.
		 * <p>This method must be called on the UI thread.
		 * @param refreshActive true if refresh is starting, false if ending
		 */
		protected final void setActive(boolean refreshActive) {
			active.set(refreshActive);
		}

		/**
		 * <p>Triggers the successful refresh event with the given result items
		 * <p>This method must be called on the UI thread.
		 * @param result the refresh result
		 * @see #result()
		 */
		protected final void notifyResult(Collection<T> result) {
			onResult.accept(result);
		}

		/**
		 * <p>Triggers the refresh exception event
		 * <p>This method must be called on the UI thread.
		 * @param exception the refresh exception
		 * @see #exception()
		 */
		protected final void notifyException(Exception exception) {
			onException.accept(exception);
		}

		/**
		 * @return true if we're running on a UI thread
		 */
		protected abstract boolean isUserInterfaceThread();

		/**
		 * <p>Performes an async refresh
		 * <p>This method must be called on the UI thread.
		 * @param onResult if specified will be called with the result after a successful refresh
		 */
		protected abstract void refreshAsync(@Nullable Consumer<Collection<T>> onResult);

		/**
		 * <p>Performs a sync refresh
		 * <p>This method must be called on the UI thread.
		 * @param onResult if specified will be called with the result after a successful refresh
		 */
		protected abstract void refreshSync(@Nullable Consumer<Collection<T>> onResult);

		/**
		 * <p>Processes the refresh result, by replacing the current model items by the result items.
		 * <p>This method must be called on UI thread.
		 * @param result the items resulting from the refresh operation
		 */
		protected abstract void processResult(Collection<T> result);
	}
}
