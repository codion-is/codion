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

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.utilities.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link ComboBoxModel} based on the UI-agnostic
 * {@link is.codion.common.model.component.combobox.FilterComboBoxModel}, adding the {@link ComboBoxModel}
 * interface — mirroring how {@code SwingTableModel} extends {@code TableModel}. The rich model logic
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
		return DefaultSwingFilterComboBoxModel.model(model);
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
	 * Builds a Swing {@link SwingFilterComboBoxModel} — the same options as the common
	 * {@link is.codion.common.model.component.combobox.FilterComboBoxModel.Builder} (the refresher is managed
	 * internally as a {@code ProgressWorker} based one), but the chain stays Swing-typed so {@code build()}
	 * yields a {@link ComboBoxModel}.
	 * @param <T> the item type
	 */
	interface Builder<T> {

		/**
		 * Provides a Swing {@link Builder}
		 */
		interface ItemsStep {

			/**
			 * @param <T> the item type
			 * @param items the items to add to the model
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<T> items(Collection<T> items);

			/**
			 * @param <T> the item type
			 * @param items the item supplier
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<T> items(Supplier<Collection<T>> items);

			/**
			 * @param items the items to display in the model
			 * @param <T> the item type
			 * @return a new {@link SwingItemComboBoxModelBuilder}
			 */
			<T> SwingItemComboBoxModelBuilder<T> items(List<Item<T>> items);
		}

		/**
		 * @param comparator the comparator, null for unsorted @return this builder
		 */
		Builder<T> comparator(@Nullable Comparator<T> comparator);

		/**
		 * @param includeNull true if a null item should be included @return this builder
		 */
		Builder<T> includeNull(boolean includeNull);

		/**
		 * @param nullItem the item representing null @return this builder
		 */
		Builder<T> nullItem(@Nullable T nullItem);

		/**
		 * @param item the item to select initially @return this builder
		 */
		Builder<T> select(@Nullable T item);

		/**
		 * @param translator the selected item translator @return this builder
		 */
		Builder<T> translator(Function<Object, T> translator);

		/**
		 * @param filterSelected true if the selected item should be filtered @return this builder
		 */
		Builder<T> filterSelected(boolean filterSelected);

		/**
		 * @param item receives the selected item, possibly null @return this builder
		 */
		Builder<T> onItemSelected(Consumer<@Nullable T> item);

		/**
		 * @param onRefreshException the refresh exception handler @return this builder
		 */
		Builder<T> onRefreshException(Consumer<Exception> onRefreshException);

		/**
		 * @param refresh true if the model items should be refreshed on initialization @return this builder
		 */
		Builder<T> refresh(boolean refresh);

		/**
		 * @return a new {@link SwingFilterComboBoxModel} instance
		 */
		SwingFilterComboBoxModel<T> build();
	}

	/**
	 * Builds a Swing {@link SwingFilterComboBoxModel} based on the {@link Item} class.
	 * @param <T> the item type
	 */
	interface SwingItemComboBoxModelBuilder<T> {

		/**
		 * @param sorted true if the items should be sorted @return this builder
		 */
		SwingItemComboBoxModelBuilder<T> sorted(boolean sorted);

		/**
		 * @param comparator the comparator to sort by @return this builder
		 */
		SwingItemComboBoxModelBuilder<T> sorted(Comparator<Item<T>> comparator);

		/**
		 * @param selected the item to select initially @return this builder
		 */
		SwingItemComboBoxModelBuilder<T> selected(@Nullable T selected);

		/**
		 * @param selected the item to select initially @return this builder
		 */
		SwingItemComboBoxModelBuilder<T> selected(Item<T> selected);

		/**
		 * @return a new {@link SwingFilterComboBoxModel}
		 */
		SwingFilterComboBoxModel<Item<T>> build();
	}
}
