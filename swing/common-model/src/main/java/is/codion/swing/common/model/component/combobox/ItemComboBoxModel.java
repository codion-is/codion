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

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link FilterComboBoxModel} implementations based on the {@link Item} class.
 * @see #itemComboBoxModel()
 * @see #sortedItemComboBoxModel()
 * @see #booleanItemComboBoxModel()
 */
public final class ItemComboBoxModel {

	private ItemComboBoxModel() {}

	/**
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> itemComboBoxModel() {
		return createItemComboBoxModel(null, emptyList());
	}

	/**
	 * @param items the items
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> itemComboBoxModel(List<Item<T>> items) {
		return createItemComboBoxModel(null, items);
	}

	/**
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> sortedItemComboBoxModel() {
		return sortedItemComboBoxModel(emptyList());
	}

	/**
	 * @param items the items
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> sortedItemComboBoxModel(List<Item<T>> items) {
		return createItemComboBoxModel(items);
	}

	/**
	 * @param comparator the comparator to use when sorting
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> sortedItemComboBoxModel(Comparator<Item<T>> comparator) {
		return createItemComboBoxModel(requireNonNull(comparator), emptyList());
	}

	/**
	 * @param items the items
	 * @param comparator the comparator to use when sorting
	 * @param <T> the Item value type
	 * @return a new combo box model
	 */
	public static <T> FilterComboBoxModel<Item<T>> sortedItemComboBoxModel(List<Item<T>> items, Comparator<Item<T>> comparator) {
		requireNonNull(items);
		requireNonNull(comparator);

		return createItemComboBoxModel(comparator, items);
	}

	/**
	 * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
	 * @return a Boolean based ItemComboBoxModel
	 */
	public static FilterComboBoxModel<Item<Boolean>> booleanItemComboBoxModel() {
		return booleanItemComboBoxModel("-");
	}

	/**
	 * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
	 * @param nullCaption the string representing a null value
	 * @return a Boolean based ItemComboBoxModel
	 */
	public static FilterComboBoxModel<Item<Boolean>> booleanItemComboBoxModel(String nullCaption) {
		return booleanItemComboBoxModel(nullCaption, Messages.yes(), Messages.no());
	}

	/**
	 * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
	 * @param nullCaption the string representing a null value
	 * @param trueCaption the string representing the boolean value 'true'
	 * @param falseCaption the string representing the boolean value 'false'
	 * @return a Boolean based ItemComboBoxModel
	 */
	public static FilterComboBoxModel<Item<Boolean>> booleanItemComboBoxModel(String nullCaption, String trueCaption, String falseCaption) {
		return createItemComboBoxModel(null, asList(item(null, nullCaption), item(true, trueCaption), item(false, falseCaption)));
	}

	private static <T> FilterComboBoxModel<Item<T>> createItemComboBoxModel(List<Item<T>> items) {
		FilterComboBoxModel<Item<T>> comboBoxModel = FilterComboBoxModel.filterComboBoxModel(items);
		comboBoxModel.selection().translator().set(new SelectedItemTranslator<>(comboBoxModel));

		return comboBoxModel;
	}

	private static <T> FilterComboBoxModel<Item<T>> createItemComboBoxModel(Comparator<Item<T>> comparator, Collection<Item<T>> items) {
		FilterComboBoxModel<Item<T>> comboBoxModel = FilterComboBoxModel.filterComboBoxModel();
		comboBoxModel.selection().translator().set(new SelectedItemTranslator<>(comboBoxModel));
		comboBoxModel.items().visible().comparator().set(comparator);
		comboBoxModel.items().set(items);
		if (comboBoxModel.items().contains(Item.item(null))) {
			comboBoxModel.setSelectedItem(null);
		}

		return comboBoxModel;
	}

	private static final class SelectedItemTranslator<T> implements Function<Object, Item<T>> {

		private final FilterComboBoxModel<Item<T>> comboBoxModel;

		private SelectedItemTranslator(FilterComboBoxModel<Item<T>> comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
		}

		@Override
		public Item<T> apply(Object item) {
			if (item instanceof Item) {
				return findItem((Item<T>) item);
			}

			return findItem((T) item);
		}

		private Item<T> findItem(Item<T> item) {
			int index = comboBoxModel.items().visible().indexOf(item);
			if (index >= 0) {
				return comboBoxModel.getElementAt(index);
			}

			return null;
		}

		private Item<T> findItem(T value) {
			return findItem(Item.item(value));
		}
	}
}
