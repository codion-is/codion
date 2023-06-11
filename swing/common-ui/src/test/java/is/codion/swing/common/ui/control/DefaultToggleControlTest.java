/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;

import static is.codion.swing.common.ui.component.Components.toggleButton;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultToggleControlTest {

  private final State state = State.state();
  private final Event<Boolean> valueChangeEvent = Event.event();
  private Boolean value = false;
  private boolean booleanValue;
  private Object selectedValue;

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Object getSelectedValue() {
    return selectedValue;
  }

  public void setSelectedValue(Object selectedValue) {
    this.selectedValue = selectedValue;
  }

  public void setValue(boolean value) {
    this.value = value;
    valueChangeEvent.onEvent(value);
  }

  public boolean isValue() {
    return value;
  }

  public void setNullableValue(Boolean value) {
    this.value = value;
    valueChangeEvent.onEvent(value);
  }

  public Boolean isNullableValue() {
    return value;
  }

  @Test
  void toggleControlTest() {
    ToggleControl control = ToggleControl.builder(Value.propertyValue(this, "value", boolean.class, valueChangeEvent.observer())).build();
    control.value().set(true);
    assertTrue(value);
    control.value().set(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.value().get());

    Value<Boolean> nullableValue = Value.propertyValue(this, "nullableValue", Boolean.class, valueChangeEvent.observer());
    ToggleControl nullableControl = ToggleControl.builder(nullableValue).build();
    NullableToggleButtonModel toggleButtonModel = (NullableToggleButtonModel) toggleButton()
            .toggleControl(nullableControl)
            .build()
            .getModel();
    assertTrue(toggleButtonModel.isSelected());
    assertTrue(nullableControl.value().get());
    toggleButtonModel.setSelected(false);
    assertFalse(nullableControl.value().get());
    toggleButtonModel.setSelected(true);
    assertTrue(nullableControl.value().get());
    nullableValue.set(false);
    assertFalse(nullableControl.value().get());
    nullableValue.set(null);
    assertNull(toggleButtonModel.getState());
    assertFalse(toggleButtonModel.isSelected());

    Value<Boolean> nonNullableValue = Value.value(true, false);
    ToggleControl nonNullableControl = ToggleControl.builder(nonNullableValue).build();
    ButtonModel buttonModel = toggleButton()
            .toggleControl(nonNullableControl)
            .build()
            .getModel();
    assertFalse(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nonNullableControl.value().get());
    nonNullableValue.set(false);
    assertFalse(nonNullableControl.value().get());
    nonNullableValue.set(null);
    assertFalse(nonNullableControl.value().get());

    State state = State.state(true);
    ToggleControl toggleControl = ToggleControl.toggleControl(state);
    assertTrue(toggleControl.value().get());
    JToggleButton toggleButton = ToggleButtonBuilder.builder()
            .toggleControl(toggleControl)
            .build();
    assertTrue(toggleButton.isSelected());
  }

  @Test
  void stateToggleControl() {
    State enabledState = State.state(false);
    ToggleControl control = ToggleControl.builder(state).name("stateToggleControl").enabledState(enabledState).build();
    ButtonModel buttonModel = toggleButton()
            .toggleControl(control)
            .build()
            .getModel();
    assertFalse(control.isEnabled());
    assertFalse(buttonModel.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    assertTrue(buttonModel.isEnabled());
    assertEquals(control.getName(), "stateToggleControl");
    assertFalse(control.value().get());
    state.set(true);
    assertTrue(control.value().get());
    state.set(false);
    assertFalse(control.value().get());
    control.value().set(true);
    assertTrue(state.get());
    control.value().set(false);
    assertFalse(state.get());

    enabledState.set(false);
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
  }

  @Test
  void nullableToggleControl() {
    ToggleControl toggleControl = ToggleControl.builder(Value.propertyValue(this, "nullableValue", Boolean.class, valueChangeEvent)).build();
    NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) toggleButton()
            .toggleControl(toggleControl)
            .build()
            .getModel();
    buttonModel.setState(null);
    assertNull(value);
    buttonModel.setSelected(false);
    assertFalse(value);
    buttonModel.setSelected(true);
    assertTrue(value);
    buttonModel.setState(null);
    assertNull(value);

    setNullableValue(false);
    assertFalse(buttonModel.isSelected());
    assertFalse(buttonModel.getState());
    setNullableValue(true);
    assertTrue(buttonModel.isSelected());
    assertTrue(buttonModel.getState());
    setNullableValue(null);
    assertFalse(buttonModel.isSelected());
    assertNull(buttonModel.getState());
  }

  @Test
  void checkBox() {
    JCheckBox box = CheckBoxBuilder.builder()
            .toggleControl(ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event()))
            .name("Test"))
            .build();
    assertEquals("Test", box.getText());
  }

  @Test
  void checkBoxMenuItem() {
    JMenuItem item = CheckBoxMenuItemBuilder.builder()
            .toggleControl(ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event()))
            .name("Test"))
            .build();
    assertEquals("Test", item.getText());
  }
}
