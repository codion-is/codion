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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class SelectedValue<T, C extends JComboBox<T>> extends AbstractComponentValue<T, C> {

	SelectedValue(C comboBox) {
		super(comboBox);
		if (comboBox.getModel() instanceof FilterComboBoxModel) {
			//ItemListener does not get notified when null values are selected/deselected
			((FilterComboBoxModel<T>) comboBox.getModel()).selection().item().addListener(this::notifyListeners);
		}
		else {
			comboBox.addItemListener(new NotifyOnItemSelectedListener());
		}
	}

	@Override
	protected @Nullable T getComponentValue() {
		ComboBoxModel<T> comboBoxModel = component().getModel();
		if (comboBoxModel instanceof FilterComboBoxModel) {
			return ((FilterComboBoxModel<T>) comboBoxModel).selection().item().get();
		}

		return (T) comboBoxModel.getSelectedItem();
	}

	@Override
	protected void setComponentValue(@Nullable T value) {
		component().getModel().setSelectedItem(value);
	}

	private final class NotifyOnItemSelectedListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				notifyListeners();
			}
		}
	}
}
