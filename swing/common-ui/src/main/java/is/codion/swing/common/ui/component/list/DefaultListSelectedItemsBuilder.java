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

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

final class DefaultListSelectedItemsBuilder<T> extends AbstractListBuilder<T, List<T>, ListBuilder.SelectedItems<T>>
				implements ListBuilder.SelectedItems<T> {

	DefaultListSelectedItemsBuilder(ListModel<T> listModel, Value<List<T>> linkedValue) {
		super(listModel, linkedValue);
	}

	@Override
	protected JList<T> createComponent() {
		JList<T> list = createList();
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		return list;
	}

	@Override
	protected ComponentValue<List<T>, JList<T>> createComponentValue(JList<T> component) {
		return new ListSelectedItemsValue<>(component);
	}

	@Override
	protected void setInitialValue(JList<T> component, List<T> initialValue) {
		ListSelectedItemsValue.selectValues(component, initialValue);
	}

	private static final class ListSelectedItemsValue<T> extends AbstractComponentValue<List<T>, JList<T>> {

		private ListSelectedItemsValue(JList<T> list) {
			super(list);
			list.addListSelectionListener(e -> notifyListeners());
		}

		@Override
		protected List<T> getComponentValue() {
			return component().getSelectedValuesList();
		}

		@Override
		protected void setComponentValue(List<T> value) {
			selectValues(component(), value);
		}

		private static <T> void selectValues(JList<T> list, List<T> valueSet) {
			list.setSelectedIndices(valueSet.stream()
							.map(value -> indexOf(list, value))
							.filter(OptionalInt::isPresent)
							.mapToInt(OptionalInt::getAsInt)
							.toArray());
		}

		private static <T> OptionalInt indexOf(JList<T> list, T element) {
			ListModel<T> model = list.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				if (Objects.equals(model.getElementAt(i), element)) {
					return OptionalInt.of(i);
				}
			}

			return OptionalInt.empty();
		}
	}
}
