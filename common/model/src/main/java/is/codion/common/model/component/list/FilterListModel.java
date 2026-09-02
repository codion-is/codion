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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.list;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.selection.MultiSelection;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A UI-agnostic list model based on {@link FilterModel}. The Swing-specific
 * {@code is.codion.swing.common.model.component.list.SwingFilterListModel} extends this with
 * {@code javax.swing.ListModel} (mirroring how {@code SwingFilterTableModel} extends {@code TableModel}).
 * @param <T> the item type
 * @see #builder()
 */
public interface FilterListModel<T> extends FilterModel<T> {

	@Override
	MultiSelection<T> selection();

	@Override
	FilterListSort<T> sort();

	/**
	 * @return a {@link Builder.ItemsStep} instance
	 */
	static Builder.ItemsStep builder() {
		return DefaultFilterListModel.DefaultBuilder.ITEMS;
	}

	/**
	 * Builds a {@link FilterListModel}
	 * @param <T> the item type
	 * @param <B> the builder type
	 * @see AbstractFilterListModelBuilder
	 */
	interface Builder<T, B extends Builder<T, B>> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ItemsStep {

			/**
			 * @param <T> the item type
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<T, ?> items();

			/**
			 * @param <T> the item type
			 * @param items the items to add to the model
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<T, ?> items(Collection<T> items);

			/**
			 * @param <T> the item type
			 * @param items the item supplier
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<T, ?> items(Supplier<Collection<T>> items);
		}

		/**
		 * @param comparator the comparator to use when sorting
		 * @return this builder instance
		 */
		B comparator(@Nullable Comparator<T> comparator);

		/**
		 * By default, exceptions during refresh are rethrown,
		 * use this method to handle async exceptions differently
		 * @param onRefreshException the exception handler to use during refresh
		 * @return this builder instance
		 */
		B onRefreshException(Consumer<Exception> onRefreshException);

		/**
		 * @param included the {@link Predicate} controlling which items should be included
		 * @return this builder instance
		 */
		B included(Predicate<T> included);

		/**
		 * @param listener the selection listener
		 * @return this builder instance
		 */
		B onSelectionChanged(Runnable listener);

		/**
		 * @param item receives the selected item
		 * @return this builder instance
		 */
		B onSelectedItem(Consumer<T> item);

		/**
		 * @param items receives the selected items
		 * @return this builder instance
		 */
		B onSelectedItems(Consumer<List<T>> items);

		/**
		 * @param index receives the selected index
		 * @return this builder instance
		 */
		B onSelectedIndex(Consumer<Integer> index);

		/**
		 * @param indexes receives the selected indexes
		 * @return this builder instance
		 */
		B onSelectedIndexes(Consumer<List<Integer>> indexes);

		/**
		 * @return a new {@link FilterListModel} instance
		 */
		FilterListModel<T> build();
	}
}
