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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * Provides a {@link Builder} for {@link FilterComboBoxModel} implementations based on the {@link Item} class.
 * Note that item combo box models are unsorted by default, the provided items are assumed to be ordered.
 * Use {@link Builder#sorted(boolean)} or {@link Builder#sorted(Comparator)} for a sorted combo box model.
 * @see #builder(List)
 * @see #booleanItems()
 * @see #booleanItems(String)
 * @see #booleanItems(String, String, String)
 */
public final class ItemComboBoxModel {

	private ItemComboBoxModel() {}

	/**
	 * Returns a {@link Builder}, by default unsorted.
	 * @param items the items to display in the model
	 * @param <T> the item type
	 * @return a new {@link Builder}
	 */
	public static <T> Builder<T> builder(List<Item<T>> items) {
		return new DefaultBuilder<>(items);
	}

	/**
	 * @return items for null, true and false, using the default captions
	 * @see FilterComboBoxModel#NULL_CAPTION
	 * @see Messages#yes()
	 * @see Messages#no()
	 */
	public static List<Item<Boolean>> booleanItems() {
		return booleanItems(FilterComboBoxModel.NULL_CAPTION.get());
	}

	/**
	 * @param nullCaption the caption for the null value
	 * @return items for null, true and false, using the given null caption and the default true/false captions
	 * @see Messages#yes()
	 * @see Messages#no()
	 */
	public static List<Item<Boolean>> booleanItems(String nullCaption) {
		return booleanItems(nullCaption, Messages.yes(), Messages.no());
	}

	/**
	 * @param nullCaption the caption for null
	 * @param trueCaption the caption for true
	 * @param falseCaption the caption for false
	 * @return items for null, true and false
	 */
	public static List<Item<Boolean>> booleanItems(String nullCaption, String trueCaption, String falseCaption) {
		return asList(item(null, requireNonNull(nullCaption)), item(true, requireNonNull(trueCaption)), item(false, requireNonNull(falseCaption)));
	}

	/**
	 * Builds an {@link Item} based {@link FilterComboBoxModel}
	 * @param <T> the item type
	 */
	public interface Builder<T> {

		/**
		 * Default false.
		 * @param sorted true if the items should be sorted
		 * @return this builder instance
		 */
		Builder<T> sorted(boolean sorted);

		/**
		 * @param comparator the comparator to sort by
		 * @return this builder instance
		 */
		Builder<T> sorted(Comparator<Item<T>> comparator);

		/**
		 * @return a new {@link FilterComboBoxModel}
		 */
		FilterComboBoxModel<Item<T>> build();
	}

	private static final class DefaultBuilder<T> implements Builder<T> {

		private final List<Item<T>> items;

		private boolean sorted = false;
		private Comparator<Item<T>> comparator;

		private DefaultBuilder(List<Item<T>> items) {
			this.items = requireNonNull(items);
		}

		@Override
		public Builder<T> sorted(boolean sorted) {
			this.sorted = sorted;
			if (!sorted) {
				this.comparator = null;
			}
			return this;
		}

		@Override
		public Builder<T> sorted(Comparator<Item<T>> comparator) {
			this.sorted = true;
			this.comparator = requireNonNull(comparator);
			return this;
		}

		@Override
		public FilterComboBoxModel<Item<T>> build() {
			FilterComboBoxModel.Builder<Item<T>> builder = FilterComboBoxModel.builder(items)
							.translator(new SelectedItemTranslator<>(items));
			if (!sorted) {
				builder.comparator(null);
			}
			if (comparator != null) {
				builder.comparator(comparator);
			}

			FilterComboBoxModel<Item<T>> comboBoxModel = builder.build();
			if (comboBoxModel.items().contains(Item.item(null))) {
				comboBoxModel.setSelectedItem(null);
			}

			return comboBoxModel;
		}
	}

	private static final class SelectedItemTranslator<T> implements Function<Object, Item<T>> {

		private final Map<T, Item<T>> itemMap;

		private SelectedItemTranslator(List<Item<T>> items) {
			itemMap = items.stream()
							.collect(toMap(Item::value, Function.identity()));
		}

		@Override
		public Item<T> apply(Object item) {
			if (item instanceof Item) {
				return itemMap.get(((Item<T>) item).value());
			}

			return itemMap.get(item);
		}
	}
}
