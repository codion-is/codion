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
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ListSelectionModel;
import java.util.List;

import static java.util.Collections.emptyList;

final class DefaultListSelectedItemsBuilder<T> extends AbstractListBuilder<List<T>, T, ListBuilder.SelectedItems<T>>
				implements ListBuilder.SelectedItems<T> {

	private boolean nullable = false;

	DefaultListSelectedItemsBuilder(FilterListModel<T> listModel) {
		super(listModel);
	}


	@Override
	public SelectedItems<T> nullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}

	@Override
	protected FilterList<T> createComponent() {
		FilterList<T> list = createList();
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		return list;
	}

	@Override
	protected ComponentValue<FilterList<T>, List<T>> createComponentValue(FilterList<T> component) {
		return new ListSelectedItemsValue<>(component, nullable);
	}

	private static final class ListSelectedItemsValue<T> extends AbstractComponentValue<FilterList<T>, List<T>> {

		private final boolean nullable;

		private ListSelectedItemsValue(FilterList<T> list, boolean nullable) {
			super(list, nullable ? null : emptyList());
			this.nullable = nullable;
			list.model().selection().indexes().addListener(this::notifyObserver);
		}

		@Override
		protected @Nullable List<T> getComponentValue() {
			List<T> items = component().model().selection().items().get();
			if (nullable && items.isEmpty()) {
				return null;
			}

			return items;
		}

		@Override
		protected void setComponentValue(List<T> value) {
			component().model().selection().items().set(value == null ? emptyList() : value);
		}
	}
}
