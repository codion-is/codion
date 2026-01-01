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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.model.component.list.FilterListModel;

import static java.util.Objects.requireNonNull;

final class DefaultFilterListBuilderFactory<T> implements FilterList.Builder.Factory<T> {

	static final FilterList.Builder.ModelStep MODEL = new DefaultModelStep();

	private final FilterListModel<T> listModel;

	private DefaultFilterListBuilderFactory(FilterListModel<T> listModel) {
		this.listModel = requireNonNull(listModel);
	}

	@Override
	public FilterList.Builder.Items<T> items() {
		return new DefaultFilterListItemsBuilder<>(listModel);
	}

	@Override
	public FilterList.Builder.SelectedItems<T> selectedItems() {
		return new DefaultFilterListSelectedItemsBuilder<>(listModel);
	}

	@Override
	public FilterList.Builder.SelectedItem<T> selectedItem() {
		return new DefaultFilterListSelectedItemBuilder<>(listModel);
	}

	private static final class DefaultModelStep implements FilterList.Builder.ModelStep {

		@Override
		public <T> FilterList.Builder.Factory<T> model(FilterListModel<T> listModel) {
			return new DefaultFilterListBuilderFactory<>(listModel);
		}
	}
}
