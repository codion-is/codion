/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  public void test() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(textField, "title");
    assertEquals(textField, panel.getTextField());
    assertNotNull(panel.getButton());
    panel.setMaxLength(10);
    assertEquals(10, panel.getMaxLength());
    textField.setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", textField.getText());
  }

  @Test
  public void constructorNullTextComponent() {
    assertThrows(NullPointerException.class, () -> new TextInputPanel(null, ""));
  }

  @Test
  public void setTextExceedMaxLength() {
    final JTextField textField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(textField, "title");
    panel.setMaxLength(5);
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }
}
