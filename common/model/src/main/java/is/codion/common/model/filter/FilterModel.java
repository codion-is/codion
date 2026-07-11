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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.filter;

import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.model.selection.MultiSelection.IndexedItems;
import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * Specifies a data model that can be filtered to exclude some or all of the items it contains.
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
	PropertyValue<Boolean> ASYNC = booleanValue(FilterModel.class.getName() + ".async", true);

	/**
	 * @return the model items
	 */
	Items<T> items();

	/**
	 * @return the {@link SingleSelection} instance used by this model
	 */
	SingleSelection<T> selection();

	/**
	 * @return the {@link Sort} instance used by this model
	 */
	Sort<T> sort();

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
		 * @return the {@link Sort} instance used by this items instance
		 */
		Sort<T> sort();

		/**
		 * <p>Refreshes the data in this model using its {@link Refresher}.
		 * <br><br>
		 * Retains the selection and filtering. Sorts the refreshed data.
		 * @see Refresher#active()
		 * @see Refresher#result()
		 * @see Refresher#async()
		 */
		void refresh();

		/**
		 * <p>Refreshes the data in this model using its {@link Refresher}.
		 * <br><br>
		 * Retains the selection and filtering. Sorts the refreshed data.
		 * <p>Note that a refresh superseded by a subsequent refresh invokes no callbacks.
		 * @param onResult called after a successful refresh (on the UI thread when refreshed asynchronously)
		 * @see Refresher#active()
		 * @see Refresher#result()
		 * @see Refresher#async()
		 */
		void refresh(Consumer<Collection<T>> onResult);

		/**
		 * @return all items, included and filtered, in no particular order
		 */
		Collection<T> get();

		/**
		 * Sets the items, replacing the current ones.
		 * @param items the items
		 */
		void set(Collection<T> items);

		/**
		 * <p>Adds the given item to this model.
		 * <p>If the item passes the {@link IncludedItems#predicate()} it is appended
		 * to the included items, which are then sorted if sorting is enabled.
		 * <p>If the item does not pass the {@link IncludedItems#predicate()},
		 * it will be filtered right away.
		 * @param item the item to add
		 */
		void add(T item);

		/**
		 * <p>Adds the given items to this model.
		 * <p>Items that pass the {@link IncludedItems#predicate()} are appended
		 * to the included items, which are then sorted if sorting is enabled.
		 * <p>If no items pass the {@link IncludedItems#predicate()}, they will
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
		 * <p>Removes the given items from this model.
		 * @param items the items to remove from the model
		 */
		void remove(Collection<T> items);

		/**
		 * <p>Removes the items fulfilling the given predicate from this model.
		 * @param predicate the {@link Predicate} specifying the items to remove from the model
		 */
		void remove(Predicate<T> predicate);

		/**
		 * <p>Replaces the first occurrence of the given item. If the item is not found this method has no effect.
		 * <p>Note that this method respects the include predicate, so a
		 * currently filtered item may be replaced with an included item and vice versa.
		 * <p>If the included items change they are sorted if sorting is enabled.
		 * @param item the item to replace
		 * @param replacement the replacement item
		 * @see IncludedItems#predicate()
		 */
		void replace(T item, T replacement);

		/**
		 * <p>Replaces the given map keys with their respective values.
		 * <p>Note that this method respects the include predicate, so a
		 * currently filtered item may be replaced with an included item and vice versa.
		 * <p>If the included items change they are sorted if sorting is enabled.
		 * @param replacements the items to replace mapped to their replacements
		 */
		void replace(Map<T, T> replacements);

		/**
		 * Clears the items
		 */
		void clear();

		/**
		 * @return a {@link IncludedItems} providing access to the included items, in the order they appear in the model
		 */
		IncludedItems<T> included();

		/**
		 * @return a {@link FilteredItems} providing access to the filtered items
		 */
		FilteredItems<T> filtered();

		/**
		 * Returns true if the model contains the given item, as included or filtered.
		 * @param item the item
		 * @return true if this model contains the item
		 */
		boolean contains(T item);

		/**
		 * @return the total number of items, included and filtered
		 */
		int size();

		/**
		 * <p>Filters the items according to the {@link IncludedItems#predicate()}.
		 * <p>If no predicate is specified calling this method has no effect.
		 * <p>In a sorted model the included items retain their sorted order. In an unsorted model, items
		 * that are re-included by a filter change are appended and therefore may lose their original position.
		 * @see IncludedItems#predicate()
		 */
		void filter();

		/**
		 * @return a new {@link Builder.RefresherStep} instance
		 */
		static Builder.RefresherStep builder() {
			return DefaultFilterModelItems.DefaultBuilder.REFRESHER;
		}

		/**
		 * Builds a {@link FilterModel.Items} instance
		 * @param <T> the item type
		 */
		interface Builder<T> {

			/**
			 * Provides a {@link SelectionStep}
			 */
			interface RefresherStep {

				/**
				 * @param refresher the item refresher to use
				 * @param <T> the item type
				 * @return a new {@link SelectionStep} instance
				 */
				<T> SelectionStep<T> refresher(Function<Items<T>, Refresher<T>> refresher);
			}

			/**
			 * Provides a {@link SortStep}
			 * @param <T> the item type
			 */
			interface SelectionStep<T> {

				/**
				 * @param selection provides the {@link MultiSelection} instance to use
				 * @return the next stage
				 */
				SortStep<T> selection(Function<IncludedItems<T>, MultiSelection<T>> selection);
			}

			/**
			 * Provides a {@link Builder}
			 * @param <T> the item type
			 */
			interface SortStep<T> {

				/**
				 * @param sort the {@link Sort} instance to use
				 * @return the {@link Builder}
				 */
				Builder<T> sort(Sort<T> sort);
			}

			/**
			 * @param validator the item validator
			 * @return this builder
			 */
			Builder<T> validator(Predicate<T> validator);

			/**
			 * @param included the include predicate
			 * @return this builder
			 */
			Builder<T> included(IncludePredicate<T> included);

			/**
			 * @param itemsListener the {@link ItemsListener} to add
			 * @return this builder
			 */
			Builder<T> listener(ItemsListener itemsListener);

			/**
			 * @return a new {@link Items} instance
			 */
			Items<T> build();
		}
	}

	/**
	 * Controls which items should be included.
	 * Tests the predicate set as its value, but subclasses may provide additional tests.
	 */
	interface IncludePredicate<T> extends Value<Predicate<T>>, Predicate<T> {

		/**
		 * @param item the item to test
		 * @return true if the given item should be included
		 */
		default boolean test(T item) {
			Predicate<T> predicate = get();

			return predicate == null || predicate.test(item);
		}
	}

	/**
	 * @param <T> the item type
	 */
	interface IncludedItems<T> extends Observable<List<T>>, IndexedItems<T> {

		/**
		 * Returns an unmodifiable snapshot, stable and safe to iterate; it does not reflect subsequent changes
		 * to the model. Use {@link #get(int)} and {@link #size()} for indexed access, which avoids the copy.
		 * @return the included items or an empty list if all items are filtered
		 */
		@Override
		@NonNull List<T> get();

		/**
		 * @return the {@link IncludePredicate} controlling which items should be included
		 */
		IncludePredicate<T> predicate();

		/**
		 * @return an {@link Observer} notified when items have been added
		 */
		Observer<Collection<T>> added();

		/**
		 * @return the {@link SingleSelection} instance for these items
		 */
		SingleSelection<T> selection();

		/**
		 * Returns true if the given item is included
		 * @param item the item
		 * @return true if the item is included
		 */
		boolean contains(T item);

		/**
		 * @param index the row index
		 * @return the item at the given index in this model
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		T get(int index);

		/**
		 * <p>Adds the given item at the given index and sorts the included items if sorting is enabled.
		 * <p>Note that if the item does not pass the {@link IncludedItems#predicate()} it is filtered right away and the method returns false.
		 * @param index the index
		 * @param item the item to add
		 * @return true if the item was added to the included items
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		boolean add(int index, T item);

		/**
		 * <p>Adds the given items at the given index and sorts the included items if sorting is enabled.
		 * <p>Note that if an item does not pass the {@link IncludedItems#predicate()} it is filtered right away.
		 * @param index the index at which to add the items
		 * @param items the items to add
		 * @return true if one or more of the items was added to the included items
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		boolean add(int index, Collection<T> items);

		/**
		 * <p>Sets the item at the given index. Note that sorting is NOT performed after the item has been set.
		 * <p>Note that if the item does not pass the {@link IncludedItems#predicate()} this method has no effect.
		 * @param index the index
		 * @param item the item
		 * @return true if the item was set, false if it did not pass the {@link IncludedItems#predicate()}
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 * @see IncludedItems#predicate()
		 */
		boolean set(int index, T item);

		/**
		 * <p>Removes from this model the included element at the given index
		 * @param index the index of the row to be removed
		 * @return the removed item
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		T remove(int index);

		/**
		 * <p>Removes from this model all included elements whose index is between {@code fromIndex}, inclusive and {@code toIndex}, exclusive
		 * @param fromIndex index of first row to be removed
		 * @param toIndex index after last row to be removed
		 * @return the removed items
		 * @throws IndexOutOfBoundsException in case the indexes are out of bounds
		 */
		List<T> remove(int fromIndex, int toIndex);


		/**
		 * Sorts the included items using this {@link Sort} instance, preserving the selection.
		 * @see FilterModel#sort()
		 */
		void sort();

		/**
		 * Provides a way to respond to changes to the included items.
		 * <p>
		 * The index ranges reported are <b>both indices inclusive</b>, matching the standard
		 * table-model {@code fireTableRowsInserted/Updated/Deleted} contract.
		 */
		interface ItemsListener {

			/**
			 * Called when included items are inserted
			 * @param firstIndex the first index, inclusive
			 * @param lastIndex the last index, inclusive
			 */
			void inserted(int firstIndex, int lastIndex);

			/**
			 * Called when included items are updated
			 * @param firstIndex the first index, inclusive
			 * @param lastIndex the last index, inclusive
			 */
			void updated(int firstIndex, int lastIndex);

			/**
			 * Called when included items are deleted
			 * @param firstIndex the first index, inclusive
			 * @param lastIndex the last index, inclusive
			 */
			void deleted(int firstIndex, int lastIndex);

			/**
			 * Called when all items may have changed
			 */
			void changed();
		}
	}

	/**
	 * @param <T> the item type
	 */
	interface FilteredItems<T> extends Observable<Collection<T>> {

		/**
		 * Returns an unmodifiable snapshot, stable and safe to iterate; it does not reflect subsequent changes
		 * to the model.
		 * @return the filtered items or an empty collection in case of no filtered items
		 */
		@Override
		@NonNull Collection<T> get();

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
		 * @see #ASYNC
		 */
		State async();

		/**
		 * <p>Changes to this state are triggered on the UI thread when refreshed asynchronously,
		 * otherwise on the calling thread (see {@link #refresh(Consumer)}).
		 * @return an observable indicating that a refresh is in progress
		 */
		ObservableState active();

		/**
		 * <p>This event is triggered on the UI thread when refreshed asynchronously,
		 * otherwise on the calling thread (see {@link #refresh(Consumer)}).
		 * @return an observer notified with the result after a successful refresh
		 */
		Observer<Collection<T>> result();

		/**
		 * <p>Refreshes the data. Async refresh is performed when it is enabled ({@link #async()}) and this method is called on the UI thread.
		 * <p>Note that a refresh superseded by a subsequent refresh invokes no callbacks.
		 * @param onResult called with the result after a successful refresh, may be null (on the UI thread when refreshed asynchronously)
		 * @see #active()
		 * @see #result()
		 * @see #async()
		 */
		void refresh(@Nullable Consumer<Collection<T>> onResult);

		/**
		 * @param <T> the item type
		 * @return a new {@link Builder} instance
		 */
		static <T> Builder<T> builder() {
			return new DefaultRefresher.DefaultBuilder<>();
		}

		/**
		 * Builds a {@link Refresher}.
		 * @param <T> the item type
		 */
		interface Builder<T> {

			/**
			 * @param items supplies the items during refresh, null for a {@link Refresher} which does nothing
			 * @return this builder instance
			 */
			Builder<T> items(@Nullable Supplier<Collection<T>> items);

			/**
			 * @param onResult called with the result on each successful refresh, typically to replace the model items
			 * @return this builder instance
			 */
			Builder<T> onResult(@Nullable Consumer<Collection<T>> onResult);

			/**
			 * @param onException called in case of a failed refresh, rethrows by default
			 * @return this builder instance
			 */
			Builder<T> onException(@Nullable Consumer<Exception> onException);

			/**
			 * @param async true if refresh should be asynchronous when triggered on the UI thread, {@link FilterModel#ASYNC} by default
			 * @return this builder instance
			 */
			Builder<T> async(boolean async);

			/**
			 * @return a new {@link Refresher} instance
			 */
			Refresher<T> build();
		}
	}

	/**
	 * Implements the sorting for a {@link FilterModel}
	 * @param <T> the model item type
	 */
	interface Sort<T> extends Comparator<T> {

		/**
		 * @return true if sorting is active
		 */
		boolean sorted();

		/**
		 * @return an observer notified each time the sorting changes, the event data indicating whether the sort is active
		 */
		Observer<Boolean> observer();
	}
}
