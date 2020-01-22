/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;

import static org.junit.jupiter.api.Assertions.*;

public final class ControlsTest {

  private final State state = States.state();
  private final Event<Boolean> valueChangeEvent = Events.event();
  private Boolean value = false;

  public void setValue(final boolean value) {
    this.value = value;
    valueChangeEvent.onEvent(value);
  }

  public boolean isValue() {
    return value;
  }

  public void setNullableValue(final Boolean value) {
    this.value = value;
    valueChangeEvent.onEvent(value);
  }

  public Boolean isNullableValue() {
    return value;
  }

  @Test
  public void toggleControl() {
    final ToggleControl control = Controls.toggleControl(this, "value", "test", valueChangeEvent.getObserver());
    control.getValue().set(true);
    assertTrue(value);
    control.getValue().set(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getValue().get());

    final Value<Boolean> nullableValue = Values.value(true);
    final ToggleControl nullableControl = Controls.toggleControl(nullableValue);
    ButtonModel buttonModel = ControlProvider.createButtonModel(nullableControl);
    assertTrue(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nullableControl.getValue().get());
    nullableValue.set(false);
    assertFalse(nullableControl.getValue().get());
    nullableValue.set(null);
    assertNull(((NullableToggleButtonModel) buttonModel).getState());

    final Value<Boolean> nonNullableValue = Values.value(true, false);
    final ToggleControl nonNullableControl = Controls.toggleControl(nonNullableValue);
    buttonModel = ControlProvider.createButtonModel(nonNullableControl);
    assertFalse(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nonNullableControl.getValue().get());
    nonNullableValue.set(false);
    assertFalse(nonNullableControl.getValue().get());
    nonNullableValue.set(null);
    assertFalse(nonNullableControl.getValue().get());
  }

  @Test
  public void toggleControlInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> Controls.toggleControl(this, "invalid", "test", valueChangeEvent.getObserver()));
  }

  @Test
  public void stateToggleControl() {
    final State enabledState = States.state(false);
    final ToggleControl control = Controls.toggleControl(state, "stateToggleControl", enabledState);
    final ButtonModel buttonModel = ControlProvider.createButtonModel(control);
    assertFalse(control.isEnabled());
    assertFalse(buttonModel.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    assertTrue(buttonModel.isEnabled());
    assertEquals(control.getName(), "stateToggleControl");
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
  public void nullableToggleControl() {
    final ToggleControl toggleControl = Controls.toggleControl(this, "nullableValue", "nullable", valueChangeEvent, null, true);
    final NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) ControlProvider.createButtonModel(toggleControl);
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
  public void eventControl() {
    Controls.eventControl(Events.event()).actionPerformed(null);
  }
}
