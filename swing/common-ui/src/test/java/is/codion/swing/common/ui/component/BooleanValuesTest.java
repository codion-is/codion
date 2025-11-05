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
package is.codion.swing.common.ui.component;

import is.codion.common.utilities.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.booleanItems;
import static org.junit.jupiter.api.Assertions.*;

public class BooleanValuesTest {

	@Test
	void booleanToggleButtonCheckBox() {
		Value<Boolean> booleanValue = Value.nonNull(false);
		JCheckBox checkBox = Components.checkBox()
						.link(booleanValue)
						.build();
		assertFalse(checkBox.isSelected());
		booleanValue.set(true);
		assertTrue(checkBox.isSelected());
		checkBox.doClick();
		assertFalse(booleanValue.getOrThrow());
	}

	@Test
	void booleanComboBox() {
		FilterComboBoxModel<Item<Boolean>> model = FilterComboBoxModel.builder()
						.items(booleanItems())
						.build();
		model.setSelectedItem(false);
		ComponentValue<JComboBox<Item<Boolean>>, Boolean> componentValue = Components.itemComboBox()
						.model(model)
						.buildValue();
		assertFalse(componentValue.getOrThrow());
		componentValue.component().getModel().setSelectedItem(true);
		assertTrue(componentValue.getOrThrow());
		componentValue.component().getModel().setSelectedItem(null);
		assertNull(componentValue.get());
	}

	@Test
	void booleanToggleButton() {
		ComponentValue<JToggleButton, Boolean> value = Components.toggleButton()
						.buildValue();

		JToggleButton button = value.component();
		ButtonModel model = button.getModel();

		assertFalse(value.getOrThrow());
		model.setSelected(true);
		assertTrue(value.getOrThrow());
		model.setSelected(false);
		assertFalse(value.getOrThrow());

		value.set(true);
		assertTrue(model.isSelected());
	}

	@Test
	void booleanNullableToggleButton() {
		Value<Boolean> nullableBooleanValue = Value.nullable();
		ComponentValue<NullableCheckBox, Boolean> value = Components.nullableCheckBox()
						.link(nullableBooleanValue)
						.buildValue();

		NullableCheckBox checkBox = value.component();
		NullableToggleButtonModel model = checkBox.model();

		assertNull(value.get());
		model.setSelected(true);
		assertTrue(value.getOrThrow());
		model.setSelected(false);
		assertFalse(value.getOrThrow());

		value.set(true);
		assertTrue(model.isSelected());
		value.clear();
		assertNull(model.get());

		model.setSelected(false);
		assertFalse(value.getOrThrow());
		model.setSelected(true);
		assertTrue(value.getOrThrow());
		model.set(null);
		assertNull(value.get());
	}
}
