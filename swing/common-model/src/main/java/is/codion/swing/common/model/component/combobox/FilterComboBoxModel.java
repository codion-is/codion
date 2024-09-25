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
import is.codion.common.model.selection.SingleSelectionModel;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A combo box model based on {@link FilterModel}.
 * @param <T> the type of values in this combo box model
 * @see #filterComboBoxModel()
 * @see Items.Visible#predicate()
 */
public interface FilterComboBoxModel<T> extends FilterModel<T>, ComboBoxModel<T> {

	/**
	 * Specifies the caption used by default to represent null in combo box models.
	 * <li>Value type: String
	 * <li>Default value: -
	 */
	PropertyValue<String> COMBO_BOX_NULL_CAPTION = Configuration.stringValue(FilterComboBoxModel.class.getName() + ".nullCaption", "-");

	/**
	 * @return true if the model data has been cleared and needs to be refreshed
	 */
	boolean cleared();

	/**
	 * Replaces the given item in this combo box model
	 * @param item the item to replace
	 * @param replacement the replacement item
	 * @throws IllegalArgumentException in case the replacement item fails validation
	 */
	void replace(T item, T replacement);

	/**
	 * Provides a way for the model to prevent the addition of certain items.
	 * Trying to add items that fail validation will result in an exception.
	 * Note that any translation of the selected item is done before validation.
	 * @return the {@link Value} controlling the item validator
	 */
	Value<Predicate<T>> validator();

	/**
	 * Provides a way for the combo box model to translate an item when it is selected, such
	 * as selecting the String "1" in a String based model when selected item is set to the number 1.
	 * @return the {@link Value} controlling the selected item translator
	 */
	Value<Function<Object, T>> selectedItemTranslator();

	/**
	 * Provides a way for the combo box model to prevent the selection of certain items.
	 * @return the {@link Value} controlling the valid selection predicate
	 */
	Value<Predicate<T>> validSelectionPredicate();

	/**
	 * @return the {@link State} controlling whether a null value is included as the first item
	 * @see #nullItem()
	 */
	State includeNull();

	/**
	 * Controls the item that should represent the null value in this model.
	 * Note that {@link #includeNull()} must be used as well to enable the null value.
	 * @return the {@link Value} controlling the item representing null
	 * @see #includeNull()
	 */
	Value<T> nullItem();

	@Override
	FilterComboBoxSelectionModel<T> selection();

	/**
	 * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
	 * via {@link #nullItem()}, {@link FilterComboBoxSelectionModel#value()} is usually what you want
	 */
	T getSelectedItem();

	/**
	 * Specifies whether filtering the model affects the currently selected item.
	 * If true, the selection is cleared when the selected item is filtered from
	 * the model, otherwise the selected item can potentially represent a value
	 * which is not currently visible in the model
	 * This is false by default.
	 * @return the {@link State} controlling whether filtering affects the selected item
	 * @see Items.Visible#predicate()
	 */
	State filterSelectedItem();

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
	 */
	interface FilterComboBoxSelectionModel<T> extends SingleSelectionModel<T> {

		/**
		 * @return the selected value, null in case the value representing null is selected
		 * @see #nullSelected()
		 */
		T value();

		/**
		 * Returns true if this model contains null and it is selected.
		 * @return true if this model contains null and it is selected, false otherwise
		 * @see #includeNull()
		 */
		boolean nullSelected();
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
