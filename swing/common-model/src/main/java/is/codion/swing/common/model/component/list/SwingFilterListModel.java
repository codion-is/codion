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

import is.codion.common.model.component.list.FilterListModel;


import javax.swing.ListModel;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * A Swing {@link ListModel} based on the UI-agnostic
 * {@link is.codion.common.model.component.list.FilterListModel}, adding the {@link ListModel}
 * interface — mirroring how {@code SwingTableModel} extends {@code TableModel}. The rich model logic
 * (items, selection, filtering, sorting) lives in the common module; this only adds the Swing coat,
 * with {@link #selection()} narrowed to a {@link FilterListSelection} (a {@code javax.swing.ListSelectionModel}).
 * @param <T> the item type
 * @see #builder()
 */
public interface SwingFilterListModel<T> extends FilterListModel<T>, ListModel<T> {

	@Override
	FilterListSelection<T> selection();

	/**
	 * @return a {@link Builder.ItemsStep} instance
	 */
	static Builder.ItemsStep builder() {
		return DefaultSwingFilterListModel.DefaultBuilder.ITEMS;
	}

	/**
	 * Builds a {@link SwingFilterListModel}, the {@link FilterListModel.Builder} options with the selection based
	 * on a {@code javax.swing.ListSelectionModel}.
	 * @param <T> the item type
	 */
	interface Builder<T> extends FilterListModel.Builder<T, Builder<T>> {

		/**
		 * Provides a {@link Builder}
		 */
		interface ItemsStep extends FilterListModel.Builder.ItemsStep {

			@Override
			<T> Builder<T> items();

			@Override
			<T> Builder<T> items(Collection<T> items);

			@Override
			<T> Builder<T> items(Supplier<Collection<T>> items);
		}

		@Override
		SwingFilterListModel<T> build();
	}
}
