/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
  public void addKeyEventWithoutName() {
    final JTextField textField = new JTextField();
    final String actionName = textField.getClass().getSimpleName() + KeyEvent.VK_ENTER + 0 + "keyReleased";
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.addKeyEvent(textField, KeyEvent.VK_ENTER, Control.control(() -> {}));
    assertNotNull(textField.getActionMap().get(actionName));
  }
}
