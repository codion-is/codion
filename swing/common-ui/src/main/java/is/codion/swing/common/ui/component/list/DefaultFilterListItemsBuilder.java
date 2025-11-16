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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

final class DefaultFilterListItemsBuilder<T> extends AbstractFilterListBuilder<List<T>, T, FilterList.Builder.Items<T>>
				implements FilterList.Builder.Items<T> {

	private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
	private boolean nullable = false;

	DefaultFilterListItemsBuilder(FilterListModel<T> listModel) {
		super(listModel);
	}

	@Override
	public Items<T> selectionMode(int selectionMode) {
		this.selectionMode = selectionMode;
		return this;
	}

	@Override
	public Items<T> nullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}

	@Override
	protected FilterList<T> createComponent() {
		FilterList<T> list = createList();
		list.setSelectionMode(selectionMode);

		return list;
	}

	@Override
	protected ComponentValue<FilterList<T>, List<T>> createComponentValue(FilterList<T> component) {
		return new ListItemsValue<>(component, nullable);
	}

	private static final class ListItemsValue<T> extends AbstractComponentValue<FilterList<T>, List<T>> {

		private final boolean nullable;

		private ListItemsValue(FilterList<T> list, boolean nullable) {
			super(list, nullable ? null : emptyList());
			this.nullable = nullable;
			list.model().addListDataListener(new DefaultListDataNotifier());
		}

		@Override
		protected @Nullable List<T> getComponentValue() {
			Collection<T> collection = component().model().items().get();
			if (nullable && collection.isEmpty()) {
				return null;
			}

			return new ArrayList<>(collection);
		}

		@Override
		protected void setComponentValue(List<T> value) {
			component().model().items().set(value == null ? emptyList() : value);
		}

		private final class DefaultListDataNotifier implements ListDataListener {

			@Override
			public void intervalAdded(ListDataEvent e) {
				notifyObserver();
			}

			@Override
			public void intervalRemoved(ListDataEvent e) {
				notifyObserver();
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				notifyObserver();
			}
		}
	}
}
