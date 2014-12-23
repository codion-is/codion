/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.checkbox.TristateButtonModel;

import org.junit.Test;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    final ToggleControl control = Controls.toggleControl(this, "value", "test", valueChangeEvent.getObserver());
    control.getButtonModel().setSelected(true);
    assertTrue(value);
    control.getButtonModel().setSelected(false);
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
    final ToggleControl toggleControl = Controls.toggleControl(this, "tristateValue", "tristate", valueChangeEvent, null, true);
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
    final Event<ActionEvent> event = Events.event();
    final Collection<ActionEvent> firedEvents = new ArrayList<>();
    event.addInfoListener(new EventInfoListener<ActionEvent>() {
      @Override
      public void eventOccurred(final ActionEvent info) {
        firedEvents.add(info);
      }
    });
    final Control control = Controls.eventControl(event);
    final ActionEvent actionEvent = new ActionEvent(this, 0, "command");
    control.actionPerformed(actionEvent);
    assertTrue(firedEvents.contains(actionEvent));
  }
}
