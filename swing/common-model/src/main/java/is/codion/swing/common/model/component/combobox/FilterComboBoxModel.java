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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.Configuration;
import is.codion.common.model.FilterModel;
import is.codion.common.model.selection.SingleItemSelection;
import is.codion.common.observer.Mutable;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A combo box model based on {@link FilterModel}.
 * @param <T> the type of values in this combo box model
 * @see #filterComboBoxModel()
 * @see VisibleItems#predicate()
 */
public interface FilterComboBoxModel<T> extends FilterModel<T>, ComboBoxModel<T> {

	/**
	 * Specifies the caption used by default to represent the null item in combo box models.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: -
	 * </ul>
	 * @see ComboBoxItems#nullItem()
	 */
	PropertyValue<String> NULL_CAPTION = Configuration.stringValue(FilterComboBoxModel.class.getName() + ".nullCaption", "-");

	@Override
	ComboBoxItems<T> items();

	@Override
	ComboBoxSelection<T> selection();

	/**
	 * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
	 * via {@link ComboBoxItems#nullItem()}, {@link ComboBoxSelection#value()} is usually what you want
	 */
	T getSelectedItem();

	/**
	 * @param itemFinder responsible for finding the item to select
	 * @param <V> the value type
	 * @return a {@link Value} linked to the selected item using the given {@link ItemFinder} instance
	 */
	<V> Value<V> createSelectorValue(ItemFinder<T, V> itemFinder);

	/**
	 * @param <T> the item type
	 * @return a new {@link FilterComboBoxModel} instance
	 */
	static <T> FilterComboBoxModel<T> filterComboBoxModel() {
		return new DefaultFilterComboBoxModel<>();
	}

	/**
	 * @param <T> the item type
	 * @param items the items to add to the model
	 * @return a new {@link FilterComboBoxModel} instance
	 */
	static <T> FilterComboBoxModel<T> filterComboBoxModel(Collection<T> items) {
		return new DefaultFilterComboBoxModel<>(requireNonNull(items));
	}

	/**
	 * @param <T> the item type
	 * @param supplier the item supplier
	 * @return a new {@link FilterComboBoxModel} instance
	 */
	static <T> FilterComboBoxModel<T> filterComboBoxModel(Supplier<Collection<T>> supplier) {
		return new DefaultFilterComboBoxModel<>(requireNonNull(supplier));
	}

	/**
	 * Specifies the item that should represent null for providing a caption.
	 * @param <T> the item type
	 */
	interface NullItem<T> extends Mutable<T> {

		/**
		 * @return the {@link State} controlling whether a null value is included as the first item
		 */
		State include();
	}

	/**
	 * @param <T> the item type
	 */
	interface ComboBoxItems<T> extends Items<T> {

		/**
		 * Replaces the given item in this combo box model
		 * @param item the item to replace
		 * @param replacement the replacement item
		 * @throws IllegalArgumentException in case the replacement item fails validation
		 */
		void replace(T item, T replacement);

		/**
		 * @return true if the items have been cleared and need to be refreshed
		 */
		boolean cleared();

		/**
		 * @return the null item
		 */
		NullItem<T> nullItem();
	}

	/**
	 * @param <T> the item type
	 */
	interface ComboBoxSelection<T> extends SingleItemSelection<T> {

		/**
		 * @return the selected value, null in case the value representing null is selected
		 * @see #nullSelected()
		 */
		T value();

		/**
		 * Returns true if this model contains null and it is selected.
		 * @return true if this model contains null and it is selected, false otherwise
		 * @see ComboBoxItems#nullItem()
		 */
		boolean nullSelected();

		/**
		 * Provides a way for the combo box model to translate an item when it is selected, such
		 * as selecting the String "1" in a String based model when selected item is set to the number 1.
		 * @return the {@link Value} controlling the selected item translator
		 */
		Value<Function<Object, T>> translator();

		/**
		 * Specifies whether filtering the model affects the currently selected item.
		 * If true, the selection is cleared when the selected item is filtered from
		 * the model, otherwise the selected item can potentially represent a value
		 * which is not currently visible in the model
		 * This is false by default.
		 * @return the {@link State} controlling whether filtering affects the selected item
		 * @see VisibleItems#predicate()
		 */
		State filterSelected();
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
