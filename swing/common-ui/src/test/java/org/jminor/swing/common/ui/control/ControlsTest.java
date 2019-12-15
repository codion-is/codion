/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.*;

public final class ControlsTest {

  private final State state = States.state();
  private final Event<Boolean> valueChangeEvent = Events.event();
  private Boolean value = false;

  public void setValue(final boolean value) {
    this.value = value;
    valueChangeEvent.fire(value);
  }

  public boolean isValue() {
    return value;
  }

  public void setNullableValue(final Boolean value) {
    this.value = value;
    valueChangeEvent.fire(value);
  }

  public Boolean isNullableValue() {
    return value;
  }

  @Test
  public void toggleControl() {
    final Controls.ToggleControl control = Controls.toggleControl(this, "value", "test", valueChangeEvent.getObserver());
    control.getButtonModel().setSelected(true);
    assertTrue(value);
    control.getButtonModel().setSelected(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getButtonModel().isSelected());

    final Value<Boolean> nullableValue = Values.value(true);
    final Controls.ToggleControl nullableControl = Controls.toggleControl(nullableValue);
    assertTrue(nullableControl.getButtonModel() instanceof NullableToggleButtonModel);
    assertTrue(nullableControl.getButtonModel().isSelected());
    nullableValue.set(false);
    assertFalse(nullableControl.getButtonModel().isSelected());
    nullableValue.set(null);
    assertNull(((NullableToggleButtonModel) nullableControl.getButtonModel()).get());

    final Value<Boolean> nonNullableValue = Values.value(true, false);
    final Controls.ToggleControl nonNullableControl = Controls.toggleControl(nonNullableValue);
    assertFalse(nonNullableControl.getButtonModel() instanceof NullableToggleButtonModel);
    assertTrue(nonNullableControl.getButtonModel().isSelected());
    nonNullableValue.set(false);
    assertFalse(nonNullableControl.getButtonModel().isSelected());
    nonNullableValue.set(null);
    assertFalse(nonNullableControl.getButtonModel().isSelected());
  }

  @Test
  public void toggleControlInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> Controls.toggleControl(this, "invalid", "test", valueChangeEvent.getObserver()));
  }

  @Test
  public void stateToggleControl() {
    final State enabledState = States.state(false);
    final Controls.ToggleControl control = Controls.toggleControl(state, "stateToggleControl", enabledState);
    assertFalse(control.isEnabled());
    assertFalse(control.getButtonModel().isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    assertTrue(control.getButtonModel().isEnabled());
    assertEquals(control.getName(), "stateToggleControl");
    assertFalse(control.getButtonModel().isSelected());
    state.set(true);
    assertTrue(control.getButtonModel().isSelected());
    state.set(false);
    assertFalse(control.getButtonModel().isSelected());
    control.getButtonModel().setSelected(true);
    assertTrue(state.get());
    control.getButtonModel().setSelected(false);
    assertFalse(state.get());

    enabledState.set(false);
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
  }

  @Test
  public void nullableToggleControl() {
    final Controls.ToggleControl toggleControl = Controls.toggleControl(this, "nullableValue", "nullable", valueChangeEvent, null, true);
    final NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) toggleControl.getButtonModel();
    buttonModel.set(null);
    assertNull(value);
    buttonModel.setSelected(false);
    assertFalse(value);
    buttonModel.setSelected(true);
    assertTrue(value);
    buttonModel.set(null);
    assertNull(value);

    setNullableValue(false);
    assertFalse(buttonModel.isSelected());
    assertFalse(buttonModel.get());
    setNullableValue(true);
    assertTrue(buttonModel.isSelected());
    assertTrue(buttonModel.get());
    setNullableValue(null);
    assertFalse(buttonModel.isSelected());
    assertNull(buttonModel.get());
  }

  @Test
  public void eventControl() {
    Controls.eventControl(Events.event()).actionPerformed(null);
  }
}
