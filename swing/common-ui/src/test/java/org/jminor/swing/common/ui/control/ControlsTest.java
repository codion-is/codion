/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.swing.common.model.checkbox.TristateButtonModel;

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

  public void setTristateValue(final Boolean value) {
    this.value = value;
    valueChangeEvent.fire(value);
  }

  public Boolean isTristateValue() {
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
  }

  @Test
  public void toggleControlInvalidValue() {
    assertThrows(IllegalArgumentException.class, () -> Controls.toggleControl(this, "invalid", "test", valueChangeEvent.getObserver()));
  }

  @Test
  public void stateToggleControl() {
    final State enabledState = States.state(true);
    final Controls.ToggleControl control = Controls.toggleControl(state, "stateToggleControl", enabledState);
    assertEquals(control.getName(), "stateToggleControl");
    assertFalse(control.getButtonModel().isSelected());
    state.setActive(true);
    assertTrue(control.getButtonModel().isSelected());
    state.setActive(false);
    assertFalse(control.getButtonModel().isSelected());
    control.getButtonModel().setSelected(true);
    assertTrue(state.isActive());
    control.getButtonModel().setSelected(false);
    assertFalse(state.isActive());

    enabledState.setActive(false);
    assertFalse(control.isEnabled());
    enabledState.setActive(true);
    assertTrue(control.isEnabled());
  }

  @Test
  public void tristateToggleControl() {
    final Controls.ToggleControl toggleControl = Controls.toggleControl(this, "tristateValue", "tristate", valueChangeEvent, null, true);
    final TristateButtonModel buttonModel = (TristateButtonModel) toggleControl.getButtonModel();
    buttonModel.setIndeterminate();
    assertNull(value);
    buttonModel.setSelected(false);
    assertFalse(value);
    buttonModel.setSelected(true);
    assertTrue(value);
    buttonModel.setIndeterminate();
    assertNull(value);

    setTristateValue(false);
    assertFalse(buttonModel.isSelected());
    assertFalse(buttonModel.isIndeterminate());
    setTristateValue(true);
    assertTrue(buttonModel.isSelected());
    assertFalse(buttonModel.isIndeterminate());
    setTristateValue(null);
    assertTrue(buttonModel.isIndeterminate());
  }

  @Test
  public void eventControl() {
    Controls.eventControl(Events.event()).actionPerformed(null);
  }
}
