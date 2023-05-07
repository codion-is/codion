/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static java.awt.event.KeyEvent.VK_ENTER;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class KeyEventsTest {

  @Test
  void addRemoveKeyEvent() {
    JTextField textField = new JTextField();
    final String actionName = "testing";
    Control control = Control.builder(() -> {}).caption(actionName).build();
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(control);
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }

  @Test
  void addKeyEventWithoutName() {
    JTextField textField = new JTextField();
    String actionName = textField.getClass().getSimpleName() + VK_ENTER + 0 + "keyPressed";
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(Control.control(() -> {}));
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }
}
