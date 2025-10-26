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
package is.codion.swing.common.ui.component.listbox;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;

final class ListComboBox<T> extends JComboBox<T> {

	private final Value<T> itemValue;
	private final ValueSet<T> linkedValue;

	ListComboBox(FilterComboBoxModel<T> comboBoxModel, ComponentValue<? extends JComponent, T> itemValue,
							 ValueSet<T> linkedValue) {
		super(comboBoxModel);
		this.itemValue = itemValue;
		this.linkedValue = linkedValue;
		this.linkedValue.link(new ListBoxItemValue<>(itemValue, comboBoxModel));
		KeyEvents.builder()
						.keyCode(VK_INSERT)
						.action(command(this::addItem))
						.enable(itemValue.component());
		KeyEvents.builder()
						.keyCode(VK_DELETE)
						.action(command(this::removeItem))
						.enable(itemValue.component());
		KeyEvents.builder()
						.keyCode(VK_DELETE)
						.modifiers(MENU_SHORTCUT_MASK)
						.action(command(this::clear))
						.enable(itemValue.component());
	}

	@Override
	public FilterComboBoxModel<T> getModel() {
		return (FilterComboBoxModel<T>) super.getModel();
	}

	Value<T> itemValue() {
		return itemValue;
	}

	ValueSet<T> linkedValue() {
		return linkedValue;
	}

	private void addItem() {
		FilterComboBoxModel<T> comboBoxModel = getModel();
		if (!itemValue.isNull() && !comboBoxModel.items().contains(itemValue.getOrThrow())) {
			comboBoxModel.items().add(itemValue.getOrThrow());
			itemValue.clear();
			if (isPopupVisible()) {
				hidePopup();
				showPopup();
			}
		}
	}

	private void removeItem() {
		FilterComboBoxModel<T> comboBoxModel = getModel();
		int index = getSelectedIndex();
		if (index != -1) {
			T selecteditem = comboBoxModel.getSelectedItem();
			comboBoxModel.items().remove(selecteditem);
			setSelectedIndex(Math.min(index, comboBoxModel.getSize() - 1));
		}
	}

	private void clear() {
		getModel().items().clear();
	}

	private static final class ListBoxItemValue<T> extends AbstractValue<Set<T>> {

		private final Value<T> itemValue;
		private final FilterComboBoxModel<T> comboBoxModel;

		private ListBoxItemValue(Value<T> itemValue, FilterComboBoxModel<T> comboBoxModel) {
			super(emptySet());
			this.itemValue = itemValue;
			this.comboBoxModel = comboBoxModel;
			this.comboBoxModel.addListDataListener(new ComboBoxModelListener());
			itemValue.addListener(this::notifyObserver);
		}

		@Override
		protected Set<T> getValue() {
			return Stream.concat(Stream.of(itemValue.get()), IntStream.range(0, comboBoxModel.getSize())
											.mapToObj(comboBoxModel::getElementAt))
							.filter(Objects::nonNull)
							.collect(toCollection(LinkedHashSet::new));
		}

		@Override
		protected void setValue(Set<T> value) {
			comboBoxModel.items().clear();
			value.forEach(comboBoxModel.items()::add);
		}

		private final class ComboBoxModelListener implements ListDataListener {

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
