/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  public void test() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = TextInputPanel.builder(textField).dialogTitle("title").build();
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
  public void constructorNullTextComponent() {
    assertThrows(NullPointerException.class, () -> TextInputPanel.builder(null));
  }

  @Test
  public void setTextExceedMaxLength() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = TextInputPanel.builder(textField).dialogTitle("title").build();
    panel.setMaximumLength(5);
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }
}
