/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Components;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  void test() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = TextInputPanel.builder(textField).caption("caption").dialogTitle("title").build();
    assertEquals(textField, panel.getTextField());
    assertNotNull(panel.getButton());
    panel.setMaximumLength(10);
    assertEquals(10, panel.getMaximumLength());
    textField.setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", textField.getText());
  }

  @Test
  void constructorNullTextComponent() {
    assertThrows(NullPointerException.class, () -> TextInputPanel.builder(null));
  }

  @Test
  void setTextExceedMaxLength() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = TextInputPanel.builder(textField).dialogTitle("title").build();
    panel.setMaximumLength(5);
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }

  @Test
  void enabledState() throws InterruptedException {
    final State enabledState = State.state();
    final TextInputPanel inputPanel = TextInputPanel.builder(new JTextField())
            .build();
    Components.linkToEnabledState(enabledState, inputPanel);
    assertFalse(inputPanel.getTextField().isEnabled());
    assertFalse(inputPanel.getButton().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(inputPanel.getTextField().isEnabled());
    assertTrue(inputPanel.getButton().isEnabled());
  }
}
