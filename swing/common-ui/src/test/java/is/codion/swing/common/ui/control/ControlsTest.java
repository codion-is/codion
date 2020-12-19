/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.event.Events;
import is.codion.common.state.State;
import is.codion.common.state.States;
import is.codion.common.value.Nullable;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;

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
    ButtonModel buttonModel = Controls.createButtonModel(nullableControl);
    assertTrue(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nullableControl.getValue().get());
    nullableValue.set(false);
    assertFalse(nullableControl.getValue().get());
    nullableValue.set(null);
    assertNull(((NullableToggleButtonModel) buttonModel).getState());

    final Value<Boolean> nonNullableValue = Values.value(true, false);
    final ToggleControl nonNullableControl = Controls.toggleControl(nonNullableValue);
    buttonModel = Controls.createButtonModel(nonNullableControl);
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
    final ButtonModel buttonModel = Controls.createButtonModel(control);
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
    final ToggleControl toggleControl = Controls.toggleControl(this, "nullableValue", "nullable", valueChangeEvent, null, Nullable.YES);
    final NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) Controls.createButtonModel(toggleControl);
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

  @Test
  public void basics() throws Exception {
    final Control test = Controls.control(this::doNothing);
    test.setName("test");
    assertEquals("test", test.toString());
    assertEquals("test", test.getName());
    assertEquals(0, test.getMnemonic());
    test.setMnemonic(10);
    assertEquals(10, test.getMnemonic());
    assertNull(test.getIcon());
    test.setKeyStroke(null);
    test.setDescription("description");
    assertEquals("description", test.getDescription());
    test.actionPerformed(null);
  }

  @Test
  public void setEnabled() {
    final State enabledState = States.state();
    final Control control = Controls.control(this::doNothing, "control", enabledState.getObserver());
    assertEquals("control", control.getName());
    assertEquals(enabledState.getObserver(), control.getEnabledObserver());
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    enabledState.set(false);
    assertFalse(control.isEnabled());
  }

  @Test
  public void setEnabledViaMethod() {
    final Control test = Controls.control(this::doNothing);
    assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
  }

  private void doNothing() {}
}
