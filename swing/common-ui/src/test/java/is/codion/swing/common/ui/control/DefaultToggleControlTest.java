/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

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

  @Test
  void toggleControlTest() {
    Value<Boolean> booleanValue = Value.value(false, false);
    ToggleControl control = ToggleControl.builder(booleanValue).build();
    control.value().set(true);
    assertTrue(booleanValue.get());
    control.value().set(false);
    assertFalse(booleanValue.get());
    booleanValue.set(true);
    assertTrue(control.value().get());

    Value<Boolean> nullableValue = Value.value(true);
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
    State state = State.state();
    State enabledState = State.state(false);
    ToggleControl control = ToggleControl.builder(state).name("stateToggleControl").enabled(enabledState).build();
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
    Value<Boolean> value = Value.value();
    ToggleControl toggleControl = ToggleControl.builder(value).build();
    NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) toggleButton()
            .toggleControl(toggleControl)
            .build()
            .getModel();
    buttonModel.setState(null);
    assertNull(value.get());
    buttonModel.setSelected(false);
    assertFalse(value.get());
    buttonModel.setSelected(true);
    assertTrue(value.get());
    buttonModel.setState(null);
    assertNull(value.get());

    value.set(false);
    assertFalse(buttonModel.isSelected());
    assertFalse(buttonModel.getState());
    value.set(true);
    assertTrue(buttonModel.isSelected());
    assertTrue(buttonModel.getState());
    value.set(null);
    assertFalse(buttonModel.isSelected());
    assertNull(buttonModel.getState());
  }

  @Test
  void checkBox() {
    Value<Boolean> value = Value.value(false, false);
    JCheckBox box = CheckBoxBuilder.builder()
            .toggleControl(ToggleControl.builder(value)
            .name("Test"))
            .build();
    assertEquals("Test", box.getText());
  }

  @Test
  void checkBoxMenuItem() {
    Value<Boolean> value = Value.value(false, false);
    JMenuItem item = CheckBoxMenuItemBuilder.builder()
            .toggleControl(ToggleControl.builder(value)
            .name("Test"))
            .build();
    assertEquals("Test", item.getText());
  }
}
