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
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel;

import org.jspecify.annotations.Nullable;

import javax.swing.ListModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A {@link ListModel} based on {@link FilterModel}.
 * <p>For instances use @link #builder()}.
 * @param <T> the item type
 * @see #builder()
 */
public interface FilterListModel<T> extends ListModel<T>, FilterModel<T> {

	@Override
	FilterListSelection<T> selection();

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
	 */
	interface Builder<T> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ItemsStep {

			/**
			 * @param <T> the item type
			 * @return a new {@link Builder} instance
			 */
			<T> FilterListModel.Builder<T> items();

			/**
			 * @param <T> the item type
			 * @param items the items to add to the model
			 * @return a new {@link Builder} instance
			 */
			<T> FilterListModel.Builder<T> items(Collection<T> items);

			/**
			 * @param <T> the item type
			 * @param items the item supplier
			 * @return a new {@link FilterListModel.Builder} instance
			 */
			<T> FilterListModel.Builder<T> items(Supplier<Collection<T>> items);
		}

		/**
		 * @param comparator the comparator to use when sorting
		 * @return this builder instance
		 */
		Builder<T> comparator(@Nullable Comparator<T> comparator);

		/**
		 * @param async true if async refresh should be enabled
		 * @return this builder instance
		 */
		Builder<T> async(boolean async);

		/**
		 * By default, exceptions during refresh are rethrown,
		 * use this method to handle async exceptions differently
		 * @param onRefreshException the exception handler to use during refresh
		 * @return this builder instance
		 */
		Builder<T> onRefreshException(Consumer<Exception> onRefreshException);

		/**
		 * @param included the {@link Predicate} controlling which items should be included
		 * @return this builder instance
		 */
		Builder<T> included(Predicate<T> included);

		/**
		 * @param listener the selection listener
		 * @return this builder instance
		 */
		Builder<T> onSelectionChanged(Runnable listener);

		/**
		 * @param item receives the selected item
		 * @return this builder instance
		 */
		Builder<T> onItemSelected(Consumer<T> item);

		/**
		 * @param items receives the selected items
		 * @return this builder instance
		 */
		Builder<T> onItemsSelected(Consumer<List<T>> items);

		/**
		 * @param index receives the selected index
		 * @return this builder instance
		 */
		Builder<T> onIndexSelected(Consumer<Integer> index);

		/**
		 * @param indexes receives the selected indexes
		 * @return this builder instance
		 */
		Builder<T> onIndexesSelected(Consumer<List<Integer>> indexes);

		/**
		 * @param selection receives the list model selection instance
		 * @return this builder instance
		 */
		Builder<T> selection(Consumer<FilterListSelection<T>> selection);

		/**
		 * @return a new {@link FilterListModel} instance
		 */
		FilterListModel<T> build();
	}
}
