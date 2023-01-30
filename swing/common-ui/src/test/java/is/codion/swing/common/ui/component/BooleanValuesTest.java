/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import static org.junit.jupiter.api.Assertions.*;

public class BooleanValuesTest {

  private boolean booleanValue;
  private final Event<Boolean> booleanValueChangedEvent = Event.event();

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
    booleanValueChangedEvent.onEvent(booleanValue);
  }

  @Test
  void booleanToggleButtonCheckBox() throws Exception {
    JCheckBox checkBox = Components.checkBox(Value.propertyValue(this, "booleanValue", boolean.class, booleanValueChangedEvent))
            .build();
    assertFalse(checkBox.isSelected());
    setBooleanValue(true);
    assertTrue(checkBox.isSelected());
    checkBox.doClick();
    assertFalse(booleanValue);
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
