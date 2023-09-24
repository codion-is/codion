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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import static org.junit.jupiter.api.Assertions.*;

public class BooleanValuesTest {

  @Test
  void booleanToggleButtonCheckBox() {
    Value<Boolean> booleanValue = Value.value(false, false);
    JCheckBox checkBox = Components.checkBox(booleanValue)
            .build();
    assertFalse(checkBox.isSelected());
    booleanValue.set(true);
    assertTrue(checkBox.isSelected());
    checkBox.doClick();
    assertFalse(booleanValue.get());
  }

  @Test
  void booleanComboBox() {
    ItemComboBoxModel<Boolean> model = ItemComboBoxModel.booleanItemComboBoxModel();
    model.setSelectedItem(false);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue = Components.booleanComboBox(model)
            .buildValue();
    assertEquals(false, componentValue.get());
    componentValue.component().getModel().setSelectedItem(true);
    assertEquals(true, componentValue.get());
    componentValue.component().getModel().setSelectedItem(null);
    assertNull(componentValue.get());
    componentValue = Components.booleanComboBox().buildValue();
    assertNull(componentValue.get());
  }

  @Test
  void booleanToggleButton() {
    ComponentValue<Boolean, JToggleButton> value = Components.toggleButton()
            .buildValue();

    JToggleButton button = value.component();
    ButtonModel model = button.getModel();

    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
  }

  @Test
  void booleanNullableToggleButton() {
    Value<Boolean> nullableBooleanValue = Value.value();
    ComponentValue<Boolean, JCheckBox> value = Components.checkBox(nullableBooleanValue)
            .buildValue();

    NullableCheckBox checkBox = (NullableCheckBox) value.component();
    NullableToggleButtonModel model = checkBox.getNullableModel();

    assertNull(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
    value.set(null);
    assertNull(model.getState());

    model.setSelected(false);
    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setState(null);
    assertNull(value.get());
  }
}
