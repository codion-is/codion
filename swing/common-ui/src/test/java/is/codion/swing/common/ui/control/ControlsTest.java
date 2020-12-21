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
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class ControlsTest {

  private final State state = States.state();
  private final Event<Boolean> valueChangeEvent = Events.event();
  private Boolean value = false;

  private final ControlList controlList = Controls.controlList(
          Controls.control(() -> {}, "one"), Controls.control(() -> {}, "two"),
          Controls.toggleControl(this, "booleanValue", "three", Events.event())
  );
  private boolean booleanValue;
  private Object selectedValue;

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Object getSelectedValue() {
    return selectedValue;
  }

  public void setSelectedValue(final Object selectedValue) {
    this.selectedValue = selectedValue;
  }

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
  public void toggleControlTest() {
    final ToggleControl control = Controls.toggleControl(this, "value", "test", valueChangeEvent.getObserver());
    control.getValue().set(true);
    assertTrue(value);
    control.getValue().set(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getValue().get());

    final Value<Boolean> nullableValue = Values.value(true);
    final ToggleControl nullableControl = Controls.toggleControl(nullableValue);
    ButtonModel buttonModel = Controls.buttonModel(nullableControl);
    assertTrue(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nullableControl.getValue().get());
    nullableValue.set(false);
    assertFalse(nullableControl.getValue().get());
    nullableValue.set(null);
    assertNull(((NullableToggleButtonModel) buttonModel).getState());

    final Value<Boolean> nonNullableValue = Values.value(true, false);
    final ToggleControl nonNullableControl = Controls.toggleControl(nonNullableValue);
    buttonModel = Controls.buttonModel(nonNullableControl);
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
    final ButtonModel buttonModel = Controls.buttonModel(control);
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
    final NullableToggleButtonModel buttonModel = (NullableToggleButtonModel) Controls.buttonModel(toggleControl);
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

  @Test
  public void checkBox() {
    final JCheckBox box = Controls.checkBox(Controls.toggleControl(this, "booleanValue",
            "Test", Events.event()));
    assertEquals("Test", box.getText());
  }

  @Test
  public void checkBoxMenuItem() {
    final JMenuItem item = Controls.checkBoxMenuItem(Controls.toggleControl(this, "booleanValue",
            "Test", Events.event()));
    assertEquals("Test", item.getText());
  }

  @Test
  public void menuBar() {
    final ControlList base = Controls.controlList();
    base.add(controlList);

    final JMenuBar menu = Controls.menuBar(base);
    assertEquals(1, menu.getMenuCount());
    assertEquals(3, menu.getMenu(0).getItemCount());
    assertEquals("one", menu.getMenu(0).getItem(0).getText());
    assertEquals("two", menu.getMenu(0).getItem(1).getText());
    assertEquals("three", menu.getMenu(0).getItem(2).getText());

    final List<ControlList> lists = new ArrayList<>();
    lists.add(controlList);
    lists.add(base);
    Controls.menuBar(lists);
  }

  @Test
  public void popupMenu() {
    final ControlList base = Controls.controlList();
    base.add(controlList);

    Controls.popupMenu(base);
  }

  @Test
  public void horizontalButtonPanel() {
    Controls.horizontalButtonPanel(controlList);
    final JPanel base = new JPanel();
    base.add(Controls.horizontalButtonPanel(controlList));
  }

  @Test
  public void verticalButtonPanel() {
    Controls.verticalButtonPanel(controlList);
    final JPanel base = new JPanel();
    base.add(Controls.verticalButtonPanel(controlList));
  }

  @Test
  public void toolBar() {
    Controls.toolBar(controlList, JToolBar.VERTICAL);
  }

  private void doNothing() {}
}
