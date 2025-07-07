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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.model.component.list.FilterListModel;

import static java.util.Objects.requireNonNull;

final class DefaultListBuilderFactory<T> implements ListBuilder.Factory<T> {

	static final ListBuilder.ModelBuilder MODEL = new DefaultModelBuilder();

	private final FilterListModel<T> listModel;

	DefaultListBuilderFactory(FilterListModel<T> listModel) {
		this.listModel = requireNonNull(listModel);
	}

	@Override
	public ListBuilder.Items<T> items() {
		return new DefaultListItemsBuilder<>(listModel);
	}

	@Override
	public ListBuilder.SelectedItems<T> selectedItems() {
		return new DefaultListSelectedItemsBuilder<>(listModel);
	}

	@Override
	public ListBuilder.SelectedItem<T> selectedItem() {
		return new DefaultListSelectedItemBuilder<>(listModel);
	}

	private static final class DefaultModelBuilder implements ListBuilder.ModelBuilder {

		@Override
		public <T> ListBuilder.Factory<T> model(FilterListModel<T> listModel) {
			return new DefaultListBuilderFactory<>(listModel);
		}
	}
}
