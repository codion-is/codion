/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class ControlsTest {

  private final State state = State.state();
  private final Event<Boolean> valueChangeEvent = Event.event();
  private Boolean value = false;

  private final ControlList controlList = ControlList.builder().controls(
          Control.builder().command(() -> {}).name("one").build(),
          Control.builder().command(() -> {}).name("two").build(),
          ToggleControl.builder().value(Value.propertyValue(this, "booleanValue", boolean.class, Event.event())).name("three").build()).build();
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
    final ToggleControl control = ToggleControl.builder()
            .value(Value.propertyValue(this, "value", boolean.class, valueChangeEvent.getObserver())).build();
    control.getValue().set(true);
    assertTrue(value);
    control.getValue().set(false);
    assertFalse(value);
    setValue(true);
    assertTrue(control.getValue().get());

    final Value<Boolean> nullableValue = Value.value(true);
    final ToggleControl nullableControl = ToggleControl.builder().value(nullableValue).build();
    ButtonModel buttonModel = Controls.buttonModel(nullableControl);
    assertTrue(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nullableControl.getValue().get());
    nullableValue.set(false);
    assertFalse(nullableControl.getValue().get());
    nullableValue.set(null);
    assertNull(((NullableToggleButtonModel) buttonModel).getState());

    final Value<Boolean> nonNullableValue = Value.value(true, false);
    final ToggleControl nonNullableControl = ToggleControl.builder().value(nonNullableValue).build();
    buttonModel = Controls.buttonModel(nonNullableControl);
    assertFalse(buttonModel instanceof NullableToggleButtonModel);
    assertTrue(nonNullableControl.getValue().get());
    nonNullableValue.set(false);
    assertFalse(nonNullableControl.getValue().get());
    nonNullableValue.set(null);
    assertFalse(nonNullableControl.getValue().get());
  }

  @Test
  public void stateToggleControl() {
    final State enabledState = State.state(false);
    final ToggleControl control = ToggleControl.builder().state(state).name("stateToggleControl").enabledState(enabledState).build();
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
    final ToggleControl toggleControl = ToggleControl.builder().value(Value.propertyValue(this, "nullableValue", Boolean.class, valueChangeEvent)).build();
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
    final State state = State.state();
    final Event<ActionEvent> event = Event.event();
    event.addListener(() -> state.set(true));
    Control.control(event).actionPerformed(null);
    assertTrue(state.get());
  }

  @Test
  public void basics() throws Exception {
    final Control test = Control.control(this::doNothing);
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
    final State enabledState = State.state();
    final Control control = Control.builder().command(this::doNothing).name("control").enabledState(enabledState.getObserver()).build();
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
    final Control test = Control.control(this::doNothing);
    assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
  }

  @Test
  public void checkBox() {
    final JCheckBox box = Controls.checkBox(ToggleControl.builder().name("Test")
            .value(Value.propertyValue(this, "booleanValue", boolean.class, Event.event())).build());
    assertEquals("Test", box.getText());
  }

  @Test
  public void checkBoxMenuItem() {
    final JMenuItem item = Controls.checkBoxMenuItem(ToggleControl.builder().name("Test")
            .value(Value.propertyValue(this, "booleanValue", boolean.class, Event.event())).build());
    assertEquals("Test", item.getText());
  }

  @Test
  public void menuBar() {
    final ControlList base = ControlList.controlList();
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
    final ControlList base = ControlList.controlList();
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
