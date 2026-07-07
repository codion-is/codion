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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.combobox;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.selection.SingleSelection;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Objects.requireNonNull;

/**
 * A UI-agnostic combo box model based on {@link FilterModel}. The Swing-specific
 * {@code is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel} extends this with
 * {@code javax.swing.ComboBoxModel} (mirroring how {@code SwingFilterTableModel} extends {@code TableModel}).
 * @param <T> the type of values in this combo box model
 * @see #builder()
 * @see IncludedItems#predicate()
 */
public interface FilterComboBoxModel<T> extends FilterModel<T> {

	/**
	 * Specifies the caption used by default to represent the null item in combo box models.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: -
	 * </ul>
	 * @see Builder#includeNull(boolean)
	 */
	PropertyValue<String> NULL_CAPTION = stringValue(FilterComboBoxModel.class.getName() + ".nullCaption", "-");

	@Override
	ComboBoxItems<T> items();

	/**
	 * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
	 * via {@link Builder#nullItem(Object)}, {@link SingleSelection#item()} is usually what you want
	 */
	@Nullable T selectedItem();

	/**
	 * @param itemFinder responsible for finding the item to select
	 * @param <V> the value type
	 * @return a new {@link Value} linked to the selected item using the given {@link ItemFinder} instance
	 */
	<V> Value<V> selector(ItemFinder<T, V> itemFinder);

	/**
	 * @return a new {@link Builder.ItemsStep} instance
	 */
	static Builder.ItemsStep builder() {
		return DefaultFilterComboBoxModel.DefaultBuilder.ITEMS;
	}

	/**
	 * Builds a {@link FilterComboBoxModel}
	 * @param <T> the item type
	 */
	interface Builder<T> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ItemsStep {

			/**
			 * @param <T> the item type
			 * @param items the items to add to the model
			 * @return a new {@link FilterComboBoxModel.Builder} instance
			 */
			<T> FilterComboBoxModel.Builder<T> items(Collection<T> items);

			/**
			 * @param <T> the item type
			 * @param items the item supplier
			 * @return a new {@link FilterComboBoxModel.Builder} instance
			 */
			<T> FilterComboBoxModel.Builder<T> items(Supplier<Collection<T>> items);

			/**
			 * Returns a {@link ItemComboBoxModelBuilder}, by default unsorted.
			 * @param items the items to display in the model
			 * @param <T> the item type
			 * @return a new {@link ItemComboBoxModelBuilder}
			 */
			<T> ItemComboBoxModelBuilder<T> items(List<Item<T>> items);
		}

		/**
		 * @param comparator the comparator, null for unsorted
		 * @return this builder
		 */
		Builder<T> comparator(@Nullable Comparator<T> comparator);

		/**
		 * @param includeNull true if a null item should be included
		 * @return this builder
		 */
		Builder<T> includeNull(boolean includeNull);

		/**
		 * Sets {@link #includeNull(boolean)} to true if {@code nullItem} is non-null, false otherwise.
		 * @param nullItem the item representing null
		 * @return this builder
		 */
		Builder<T> nullItem(@Nullable T nullItem);

		/**
		 * @param item the item to select initially
		 * @return this builder
		 */
		Builder<T> select(@Nullable T item);

		/**
		 * Provides a way for a combo box model to translate an item received via {@link SingleSelection#item()} to an actual item to select,
		 * such as selecting the String "1" in a String based model when selected item is set to the Integer 1.
		 * @param translator the selected item translator
		 * @return this builder
		 */
		Builder<T> translator(Function<Object, T> translator);

		/**
		 * <p>Specifies whether filtering the model affects the currently selected item.
		 * If true, the selection is cleared when the selected item is excluded from
		 * the model, otherwise the selected item can potentially represent a value
		 * which is not currently included in the model
		 * @param filterSelected true if the selected item should be filtered, false by default
		 * @return this builder instance
		 * @see IncludedItems#predicate()
		 */
		Builder<T> filterSelected(boolean filterSelected);

		/**
		 * @param item receives the selected item, note that this item may be null
		 * @return this builder instance
		 */
		Builder<T> onItemSelected(Consumer<@Nullable T> item);

		/**
		 * By default, exceptions during refresh are rethrown,
		 * use this method to handle async exceptions differently
		 * @param onRefreshException the exception handler to use during refresh
		 * @return this builder instance
		 */
		Builder<T> onRefreshException(Consumer<Exception> onRefreshException);

		/**
		 * Provides the {@link FilterModel.Refresher} for this model, given its {@link ComboBoxItems}.
		 * The default {@link FilterModel.Refresher.Builder#build()} refresher is async-capable via the
		 * {@link is.codion.common.model.worker.Dispatcher} SPI wherever a UI provider is on the classpath;
		 * override this only to supply a custom refresher (for example Android's coroutine based one).
		 * @param refresher the refresher factory
		 * @return this builder instance
		 */
		Builder<T> refresher(Function<ComboBoxItems<T>, FilterModel.Refresher<T>> refresher);

		/**
		 * @param refresh true if the model items should be refreshed on initialization, false by default
		 * @return this builder instance
		 */
		Builder<T> refresh(boolean refresh);

		/**
		 * @return a new {@link FilterComboBoxModel} instance
		 */
		FilterComboBoxModel<T> build();
	}

	/**
	 * <p>Builds {@link FilterComboBoxModel} implementations based on the {@link Item} class.
	 * <p>Note that item combo box models are unsorted by default, the provided items are assumed to be ordered.
	 * <p>Use {@link #sorted(boolean)} or {@link #sorted(Comparator)} for a sorted combo box model.
	 * @param <T> the item type
	 */
	interface ItemComboBoxModelBuilder<T> {

		/**
		 * @param sorted true if the items should be sorted, false by default
		 * @return this builder instance
		 */
		ItemComboBoxModelBuilder<T> sorted(boolean sorted);

		/**
		 * @param comparator the comparator to sort by
		 * @return this builder instance
		 */
		ItemComboBoxModelBuilder<T> sorted(Comparator<Item<T>> comparator);

		/**
		 * Sets the initially selected item
		 * @param selected the item to select initially
		 * @return this builder
		 * @throws IllegalArgumentException in case the model does not contain the given item
		 */
		ItemComboBoxModelBuilder<T> selected(@Nullable T selected);

		/**
		 * Sets the initially selected item
		 * @param selected the item to select initially
		 * @return this builder
		 * @throws IllegalArgumentException in case the model does not contain the given item
		 */
		ItemComboBoxModelBuilder<T> selected(Item<T> selected);

		/**
		 * @return a new {@link FilterComboBoxModel}
		 */
		FilterComboBoxModel<Item<T>> build();
	}

	/**
	 * The items of a {@link FilterComboBoxModel}.
	 * <p>
	 * Note that the {@link #included()} items do not support indexed mutation - the index based
	 * {@code add}, {@code set} and {@code remove} methods throw {@link UnsupportedOperationException},
	 * since a combo box model manages its own item order (including the null item sentinel).
	 * @param <T> the item type
	 */
	interface ComboBoxItems<T> extends Items<T> {

		/**
		 * @return true if the items have been cleared and need to be refreshed
		 */
		boolean cleared();

		/**
		 * @return true if a null item is included, shown as the first item
		 * @see FilterComboBoxModel.Builder#includeNull(boolean)
		 * @see FilterComboBoxModel.Builder#nullItem(Object)
		 */
		boolean includesNull();

		/**
		 * @return the item representing null, possibly null itself (then rendered using {@link #NULL_CAPTION})
		 * @see FilterComboBoxModel.Builder#nullItem(Object)
		 */
		@Nullable T nullItem();
	}

	/**
	 * Responsible for finding an item of type {@link T} by a single value of type {@link V}.
	 * @param <T> the combo box model item type
	 * @param <V> the type of the value to search by
	 */
	interface ItemFinder<T, V> {

		/**
		 * Returns the value representing the given item
		 * @param item the item, never null
		 * @return the value representing the given item
		 */
		V value(T item);

		/**
		 * Returns the {@link Predicate} to use when searching for an item represented by the given value
		 * @param value the value to search for, never null
		 * @return a {@link Predicate} for finding the item that is represented by the given value
		 */
		Predicate<T> predicate(V value);

		/**
		 * Returns the first item in the given collection containing the given {@code value}. Only called for non-null {@code value}s.
		 * @param items the items to search
		 * @param value the value to search for, never null
		 * @return the first item in the given list containing the given value, an empty Optional if none is found.
		 */
		default Optional<T> findItem(Collection<T> items, V value) {
			requireNonNull(value);

			return requireNonNull(items).stream()
							.filter(predicate(value))
							.findFirst();
		}
	}
}
