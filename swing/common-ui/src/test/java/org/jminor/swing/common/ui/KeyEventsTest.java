package org.jminor.swing.common.ui;

import org.jminor.swing.common.ui.control.Controls;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class KeyEventsTest {

  @Test
  public void addKeyEventWithoutName() {
    final JTextField textField = new JTextField();
    final String actionName = textField.getClass().getSimpleName() + KeyEvent.VK_ENTER + 0 + "true";
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.addKeyEvent(textField, KeyEvent.VK_ENTER, Controls.control(() -> {}));
    assertNotNull(textField.getActionMap().get(actionName));
  }
}
