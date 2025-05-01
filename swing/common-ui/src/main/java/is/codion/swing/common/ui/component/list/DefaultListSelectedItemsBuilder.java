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
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.ListSelectionModel;
import java.util.List;

import static java.util.Collections.emptyList;

final class DefaultListSelectedItemsBuilder<T> extends AbstractListBuilder<T, List<T>, ListBuilder.SelectedItems<T>>
				implements ListBuilder.SelectedItems<T> {

	DefaultListSelectedItemsBuilder(FilterListModel<T> listModel, Value<List<T>> linkedValue) {
		super(listModel, linkedValue);
	}

	@Override
	protected FilterList<T> createComponent() {
		FilterList<T> list = createList();
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		return list;
	}

	@Override
	protected ComponentValue<List<T>, FilterList<T>> createComponentValue(FilterList<T> component) {
		return new ListSelectedItemsValue<>(component);
	}

	private static final class ListSelectedItemsValue<T> extends AbstractComponentValue<List<T>, FilterList<T>> {

		private ListSelectedItemsValue(FilterList<T> list) {
			super(list, emptyList());
			list.model().selection().indexes().addListener(this::notifyListeners);
		}

		@Override
		protected List<T> getComponentValue() {
			return component().model().selection().items().get();
		}

		@Override
		protected void setComponentValue(List<T> value) {
			component().model().selection().items().set(value);
		}
	}
}
