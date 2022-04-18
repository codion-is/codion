/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;

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
    ToggleControl control = ToggleControl.builder(Value.propertyValue(this, "value", boolean.class, valueChangeEvent.getObserver())).build();
    control.getValue().set(true);
    assertTrue(value);
    control.getValue().set(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getValue().get());

    Value<Boolean> nullableValue = Value.propertyValue(this, "nullableValue", Boolean.class, valueChangeEvent.getObserver());
    ToggleControl nullableControl = ToggleControl.builder(nullableValue).build();
    NullableToggleButtonModel toggleButtonModel = (NullableToggleButtonModel) nullableControl.createButtonModel();
    assertTrue(toggleButtonModel.isSelected());
    assertTrue(nullableControl.getValue().get());
    toggleButtonModel.setSelected(false);
    assertFalse(nullableControl.getValue().get());
    toggleButtonModel.setSelected(true);
    assertTrue(nullableControl.getValue().get());
    nullableValue.set(false);
    assertFalse(nullableControl.getValue().get());
    assertFalse(toggleButtonModel.isPressed());
    nullableValue.set(null);
    assertTrue(toggleButtonModel.isPressed());
    assertNull(toggleButtonModel.getState());
    assertFalse(toggleButtonModel.isSelected());

    Value<Boolean> nonNullableValue = Value.value(true, false);
    ToggleControl nonNullableControl = ToggleControl.builder(nonNullableValue).build();
    ButtonModel buttonModel = nonNullableControl.createButtonModel();
    assertFalse(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nonNullableControl.getValue().get());
    nonNullableValue.set(false);
    assertFalse(nonNullableControl.getValue().get());
    nonNullableValue.set(null);
    assertFalse(nonNullableControl.getValue().get());

    State state = State.state(true);
    ToggleControl toggleControl = ToggleControl.toggleControl(state);
    assertTrue(toggleControl.getValue().get());
    JToggleButton toggleButton = toggleControl.createToggleButton();
    assertTrue(toggleButton.isSelected());
  }

  @Test
  void stateToggleControl() {
    State enabledState = State.state(false);
    ToggleControl control = ToggleControl.builder(state).caption("stateToggleControl").enabledState(enabledState).build();
    ButtonModel buttonModel = control.createButtonModel();
    assertFalse(control.isEnabled());
    assertFalse(buttonModel.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    assertTrue(buttonModel.isEnabled());
    assertEquals(control.getCaption(), "stateToggleControl");
    assertFalse(control.getValue().get());
    state.set(true);
    assertTrue(control.getValue().get());
    state.set(false);
    assertFalse(control.getValue().get());
    control.getValue().set(true);
    assertTrue(state.get());
    control.getValue().set(false);
    assertFalse(state.get());

    enabledState.set(false);
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
  }

  @Test
  void nullableToggleControl() {
    ToggleControl toggleControl = ToggleControl.builder(Value.propertyValue(this, "nullableValue", Boolean.class, valueChangeEvent)).build();
    NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) toggleControl.createButtonModel();
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
    JCheckBox box = ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event())).caption("Test").build().createCheckBox();
    assertEquals("Test", box.getText());
  }

  @Test
  void checkBoxMenuItem() {
    JMenuItem item = ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event()))
            .caption("Test").build().createCheckBoxMenuItem();
    assertEquals("Test", item.getText());
  }
}
