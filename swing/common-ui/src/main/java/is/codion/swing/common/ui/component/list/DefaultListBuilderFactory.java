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

import is.codion.common.value.Value;

import javax.swing.ListModel;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultListBuilderFactory<T> implements ListBuilder.Factory<T> {

	private final ListModel<T> listModel;

	DefaultListBuilderFactory(ListModel<T> listModel) {
		this.listModel = requireNonNull(listModel);
	}

	@Override
	public ListBuilder.Items<T> items() {
		return new DefaultListItemsBuilder<>(listModel, null);
	}

	@Override
	public ListBuilder.Items<T> items(Value<List<T>> linkedValue) {
		return new DefaultListItemsBuilder<>(listModel, linkedValue);
	}

	@Override
	public ListBuilder.SelectedItems<T> selectedItems() {
		return new DefaultListSelectedItemsBuilder<>(listModel, null);
	}

	@Override
	public ListBuilder.SelectedItems<T> selectedItems(Value<List<T>> linkedValue) {
		return new DefaultListSelectedItemsBuilder<>(listModel, linkedValue);
	}

	@Override
	public ListBuilder.SelectedItem<T> selectedItem() {
		return new DefaultListSelectedItemBuilder<>(listModel, null);
	}

	@Override
	public ListBuilder.SelectedItem<T> selectedItem(Value<T> linkedValue) {
		return new DefaultListSelectedItemBuilder<>(listModel, linkedValue);
	}
}
