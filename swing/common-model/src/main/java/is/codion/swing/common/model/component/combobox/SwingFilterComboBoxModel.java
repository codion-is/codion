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
package is.codion.swing.common.model.component.combobox;

import is.codion.common.i18n.Messages;
import is.codion.common.model.component.combobox.FilterComboBoxModel;
import is.codion.common.utilities.item.Item;


import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.common.utilities.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link ComboBoxModel} based on the UI-agnostic
 * {@link is.codion.common.model.component.combobox.FilterComboBoxModel}, adding the {@link ComboBoxModel}
 * interface — mirroring how {@code SwingFilterTableModel} extends {@code TableModel}. The rich model logic
 * (items, selection, filtering) lives in the common module; this only adds the Swing coat.
 * @param <T> the type of values in this combo box model
 * @see #builder()
 */
public interface SwingFilterComboBoxModel<T> extends FilterComboBoxModel<T>, ComboBoxModel<T> {

	/**
	 * @return a new {@link Builder.ItemsStep} instance
	 */
	static Builder.ItemsStep builder() {
		return DefaultSwingFilterComboBoxModel.DefaultBuilder.ITEMS;
	}

	/**
	 * Wraps the given UI-agnostic {@link FilterComboBoxModel} with the Swing {@link ComboBoxModel} coat — used to give
	 * an already-built common model (e.g. an entity combo box model) its {@code ComboBoxModel} surface without rebuilding it.
	 * @param model the model to wrap
	 * @param <T> the item type
	 * @return a new {@link SwingFilterComboBoxModel} delegating to the given model
	 */
	static <T> SwingFilterComboBoxModel<T> model(FilterComboBoxModel<T> model) {
		return DefaultSwingFilterComboBoxModel.model(requireNonNull(model));
	}

	/**
	 * @return items for null, true and false, using the default captions
	 * @see #NULL_CAPTION
	 * @see Messages#yes()
	 * @see Messages#no()
	 */
	static List<Item<Boolean>> booleanItems() {
		return booleanItems(NULL_CAPTION.getOrThrow());
	}

	/**
	 * @param nullCaption the caption for the null value
	 * @return items for null, true and false, using the given null caption and the default true/false captions
	 */
	static List<Item<Boolean>> booleanItems(String nullCaption) {
		return booleanItems(nullCaption, Messages.yes(), Messages.no());
	}

	/**
	 * @param nullCaption the caption for null
	 * @param trueCaption the caption for true
	 * @param falseCaption the caption for false
	 * @return items for null, true and false
	 */
	static List<Item<Boolean>> booleanItems(String nullCaption, String trueCaption, String falseCaption) {
		return asList(
						item(null, requireNonNull(nullCaption)),
						item(true, requireNonNull(trueCaption)),
						item(false, requireNonNull(falseCaption)));
	}

	/**
	 * Builds a {@link SwingFilterComboBoxModel}, the {@link FilterComboBoxModel.Builder} options.
	 * @param <T> the item type
	 */
	interface Builder<T> extends FilterComboBoxModel.Builder<T, Builder<T>> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ItemsStep extends FilterComboBoxModel.Builder.ItemsStep {

			@Override
			<T> Builder<T> items(Collection<T> items);

			@Override
			<T> Builder<T> items(Supplier<Collection<T>> items);

			@Override
			<T> SwingItemComboBoxModelBuilder<T> items(List<Item<T>> items);
		}

		@Override
		SwingFilterComboBoxModel<T> build();
	}

	/**
	 * Builds a {@link SwingFilterComboBoxModel} based on the {@link Item} class, the {@link ItemComboBoxModelBuilder} options.
	 * @param <T> the item type
	 */
	interface SwingItemComboBoxModelBuilder<T> extends ItemComboBoxModelBuilder<T, SwingItemComboBoxModelBuilder<T>> {

		@Override
		SwingFilterComboBoxModel<Item<T>> build();
	}
}
