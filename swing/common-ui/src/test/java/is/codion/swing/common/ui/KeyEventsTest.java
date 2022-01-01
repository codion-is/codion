/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class KeyEventsTest {

  @Test
  void addRemoveKeyEvent() {
    final JTextField textField = new JTextField();
    final String actionName = "testing";
    final Control control = Control.builder(() -> {}).caption(actionName).build();
    assertNull(textField.getActionMap().get(actionName));
    final KeyEvents.Builder builder = KeyEvents.builder(KeyEvent.VK_ENTER).action(control);
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }

  @Test
  void addKeyEventWithoutName() {
    final JTextField textField = new JTextField();
    final String actionName = textField.getClass().getSimpleName() + KeyEvent.VK_ENTER + 0 + "keyReleased";
    assertNull(textField.getActionMap().get(actionName));
    final KeyEvents.Builder builder = KeyEvents.builder(KeyEvent.VK_ENTER).action(Control.control(() -> {}));
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }
}
