/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  void test() {
    JTextField textField = new JTextField();
    TextInputPanel panel = TextInputPanel.builder(textField)
            .caption("caption")
            .dialogTitle("title")
            .build();
    assertEquals(textField, panel.getTextField());
    assertNotNull(panel.getButton());
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
    JTextField textField = new JTextField();
    TextInputPanel panel = TextInputPanel.builder(textField)
            .maximumLength(5)
            .dialogTitle("title")
            .build();
    panel.setText("12345");
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }

  @Test
  void enabledState() throws InterruptedException {
    State enabledState = State.state();
    TextInputPanel inputPanel = TextInputPanel.builder(new JTextField())
            .build();
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(inputPanel.getTextField().isEnabled());
    assertFalse(inputPanel.getButton().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(inputPanel.getTextField().isEnabled());
    assertTrue(inputPanel.getButton().isEnabled());
  }
}
