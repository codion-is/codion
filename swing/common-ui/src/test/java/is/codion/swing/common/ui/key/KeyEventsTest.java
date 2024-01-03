/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.key;

import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import static java.awt.event.KeyEvent.VK_ENTER;
import static org.junit.jupiter.api.Assertions.*;

public class KeyEventsTest {

  @Test
  void addRemoveKeyEvent() {
    JTextField textField = new JTextField();
    final String actionName = "testing";
    Control control = Control.builder(() -> {}).name(actionName).build();
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(control);
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }

  @Test
  void addKeyEventWithoutName() {
    JComboBox<String> comboBox = new JComboBox<>();
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(Control.control(() -> {})).onKeyRelease(true);
    builder.enable(comboBox);
    builder.disable(comboBox);
  }

  @Test
  void actionMissing() {
    assertThrows(IllegalStateException.class, () -> KeyEvents.builder(VK_ENTER).enable(new JTextField()));
  }
}
