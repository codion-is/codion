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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ListSelectionModel;

final class DefaultListSelectedItemBuilder<T> extends AbstractListBuilder<T, T, ListBuilder.SelectedItem<T>> implements ListBuilder.SelectedItem<T> {

	DefaultListSelectedItemBuilder(FilterListModel<T> listModel) {
		super(listModel);
	}

	@Override
	protected FilterList<T> createComponent() {
		FilterList<T> list = createList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		return list;
	}

	@Override
	protected ComponentValue<T, FilterList<T>> createComponentValue(FilterList<T> component) {
		return new ListSelectedItemValue<>(component);
	}

	private static final class ListSelectedItemValue<T> extends AbstractComponentValue<T, FilterList<T>> {

		private ListSelectedItemValue(FilterList<T> list) {
			super(list);
			list.model().selection().indexes().addListener(this::notifyListeners);
		}

		@Override
		protected @Nullable T getComponentValue() {
			return component().model().selection().item().get();
		}

		@Override
		protected void setComponentValue(@Nullable T value) {
			component().model().selection().item().set(value);
		}
	}
}
