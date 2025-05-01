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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel;
import is.codion.swing.common.model.component.list.DefaultFilterListModel.DefaultBuilder;

import javax.swing.ListModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * <p>A {@link ListModel} based on {@link FilterModel}.
 * <p>For instances use {@link #filterListModel()} or {@link #builder()}.
 * @param <T> the item type
 * @see #filterListModel()
 * @see #filterListModel(Collection)
 * @see #builder()
 * @see #builder(Collection)
 */
public interface FilterListModel<T> extends ListModel<T>, FilterModel<T> {

	@Override
	FilterListSelection<T> selection();

	@Override
	FilterListSort<T> sort();

	/**
	 * @return a new {@link FilterListModel} instance
	 * @param <T> the item type
	 */
	static <T> FilterListModel<T> filterListModel() {
		return FilterListModel.<T>builder().build();
	}

	/**
	 * @return a new {@link FilterListModel} instance
	 * @param items the items
	 * @param <T> the item type
	 */
	static <T> FilterListModel<T> filterListModel(Collection<T> items) {
		return FilterListModel.builder(items).build();
	}

	/**
	 * @return a new {@link Builder} instance
	 * @param <T> the item type
	 */
	static <T> Builder<T> builder() {
		return new DefaultBuilder<>(emptyList());
	}

	/**
	 * @return a new {@link Builder} instance
	 * @param items the items
	 * @param <T> the item type
	 */
	static <T> Builder<T> builder(Collection<T> items) {
		return new DefaultBuilder<>(requireNonNull(items));
	}

	/**
	 * Builds a {@link FilterListModel}
	 * @param <T> the item type
	 */
	interface Builder<T> {

		/**
		 * @param supplier supplies the items
		 * @return this builder instance
		 */
		Builder<T> supplier(Supplier<? extends Collection<T>> supplier);

		/**
		 * @param comparator the comparator to use when sorting
		 * @return this builder instance
		 */
		Builder<T> comparator(Comparator<T> comparator);

		/**
		 * @param asyncRefresh true if async refresh should be enabled
		 * @return this builder instance
		 */
		Builder<T> asyncRefresh(boolean asyncRefresh);

		/**
		 * @return a new {@link FilterListModel} instance
		 */
		FilterListModel<T> build();
	}
}
