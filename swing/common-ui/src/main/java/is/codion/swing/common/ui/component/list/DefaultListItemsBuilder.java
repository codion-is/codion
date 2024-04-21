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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class DefaultListItemsBuilder<T> extends AbstractListBuilder<T, List<T>, ListBuilder.Items<T>> implements ListBuilder.Items<T> {

	private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

	DefaultListItemsBuilder(ListModel<T> listModel, Value<List<T>> linkedValue) {
		super(listModel, linkedValue);
	}

	@Override
	public Items<T> selectionMode(int selectionMode) {
		this.selectionMode = selectionMode;
		return this;
	}

	@Override
	protected JList<T> createComponent() {
		JList<T> list = createList();
		list.setSelectionMode(selectionMode);

		return list;
	}

	@Override
	protected ComponentValue<List<T>, JList<T>> createComponentValue(JList<T> component) {
		return new ListItemsValue<>(component);
	}

	@Override
	protected void setInitialValue(JList<T> component, List<T> initialValue) {
		ListItemsValue.setItems(component, initialValue);
	}

	private static final class ListItemsValue<T> extends AbstractComponentValue<List<T>, JList<T>> {

		private ListItemsValue(JList<T> list) {
			super(list, Collections.emptyList());
			list.getModel().addListDataListener(new DefaultListDataNotifier());
		}

		@Override
		protected List<T> getComponentValue() {
			return new ArrayList<>(getItems());
		}

		@Override
		protected void setComponentValue(List<T> value) {
			setItems(component(), value);
		}

		private Collection<T> getItems() {
			DefaultListModel<T> listModel = (DefaultListModel<T>) component().getModel();

			return IntStream.range(0, listModel.getSize())
							.mapToObj(listModel::getElementAt)
							.collect(Collectors.toList());
		}

		private static <T> void setItems(JList<T> list, List<T> items) {
			DefaultListModel<T> listModel = (DefaultListModel<T>) list.getModel();
			listModel.removeAllElements();
			if (items != null) {
				listModel.addAll(items);
			}
		}

		private final class DefaultListDataNotifier implements ListDataListener {

			@Override
			public void intervalAdded(ListDataEvent e) {
				notifyListeners();
			}

			@Override
			public void intervalRemoved(ListDataEvent e) {
				notifyListeners();
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				notifyListeners();
			}
		}
	}
}
