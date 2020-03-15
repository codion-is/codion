/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.item.Item;
import org.jminor.common.value.Value;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import static org.junit.jupiter.api.Assertions.*;

public class BooleanValuesTest {

  private boolean booleanValue;
  private final Event booleanValueChangedEvent = Events.event();

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    booleanValueChangedEvent.onEvent();
  }

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    BooleanValues.booleanValueLink(checkBox.getModel(), this, "booleanValue", booleanValueChangedEvent);
    assertFalse(checkBox.isSelected());
    setBooleanValue(true);
    assertTrue(checkBox.isSelected());
    checkBox.doClick();
    assertFalse(booleanValue);
  }

  @Test
  public void booleanValue() {
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue = BooleanValues.booleanComboBoxValue(false);
    assertEquals(false, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(true);
    assertEquals(true, componentValue.get());
    componentValue.getComponent().getModel().setSelectedItem(null);
    assertNull(componentValue.get());
    componentValue = new BooleanComboBoxValue(new JComboBox<>(new BooleanComboBoxModel()));
    assertNull(componentValue.get());
  }

  @Test
  public void toggleUiValue() {
    final ButtonModel model = new DefaultButtonModel();
    final Value<Boolean> value = BooleanValues.booleanButtonModelValue(model);

    assertFalse(value.get());
    model.setSelected(true);
    assertTrue(value.get());
    model.setSelected(false);
    assertFalse(value.get());

    value.set(true);
    assertTrue(model.isSelected());
  }

  @Test
  public void nullableToggleUiValue() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel();
    final Value<Boolean> value = BooleanValues.booleanButtonModelValue(model);

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
