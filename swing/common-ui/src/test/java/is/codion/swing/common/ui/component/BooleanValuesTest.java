/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
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
    JCheckBox checkBox = new JCheckBox();
    ComponentValues.toggleButton(checkBox)
            .link(Value.propertyValue(this, "booleanValue", boolean.class, booleanValueChangedEvent));
    assertFalse(checkBox.isSelected());
    setBooleanValue(true);
    assertTrue(checkBox.isSelected());
    checkBox.doClick();
    assertFalse(booleanValue);
  }

  @Test
  void booleanComboBox() {
    ItemComboBoxModel<Boolean> model = ItemComboBoxModel.createBooleanModel();
    model.setSelectedItem(false);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue = ComponentValues.booleanComboBox(new JComboBox<>(model));
    assertEquals(false, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(true);
    assertEquals(true, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(null);
    assertNull(componentValue.get());
    componentValue = new BooleanComboBoxValue(new JComboBox<>(ItemComboBoxModel.createBooleanModel()));
    assertNull(componentValue.get());
  }

  @Test
  void booleanToggleButton() {
    ButtonModel model = new DefaultButtonModel();
    JToggleButton button = new JToggleButton();
    button.setModel(model);
    Value<Boolean> value = ComponentValues.toggleButton(button);

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
    NullableToggleButtonModel model = new NullableToggleButtonModel();
    NullableCheckBox checkBox = new NullableCheckBox(model);
    Value<Boolean> value = ComponentValues.toggleButton(checkBox);

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
