/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.checkbox.TristateButtonModel;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ControlsTest {

  private final State state = States.state();
  private final Event<Boolean> valueChangeEvent = Events.event();
  private boolean value = false;

  public void setValue(final boolean value) {
    this.value = value;
    valueChangeEvent.fire(value);
  }

  public boolean isValue() {
    return value;
  }

  @Test
  public void toggleControl() {
    final ToggleControl control = Controls.toggleControl(this, "value", "test", valueChangeEvent.getObserver());
    control.actionPerformed(null);
    assertTrue(value);
    control.actionPerformed(null);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getButtonModel().isSelected());
  }

  @Test(expected = IllegalArgumentException.class)
  public void toggleControlInvalidValue() {
    Controls.toggleControl(this, "invalid", "test", valueChangeEvent.getObserver());
  }

  @Test
  public void stateToggleControl() {
    final State enabledState = States.state(true);
    final ToggleControl control = Controls.toggleControl(state, enabledState);
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
    final TristateButtonModel buttonModel = new TristateButtonModel();
    final ToggleControl control = new ToggleControl("test", buttonModel, null);
    assertFalse(buttonModel.isSelected());
    control.actionPerformed(null);
    assertTrue(buttonModel.isSelected());
    control.actionPerformed(null);
    assertTrue(buttonModel.isIndeterminate());
    control.actionPerformed(null);
    assertFalse(buttonModel.isSelected());
  }
}
