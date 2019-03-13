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
    final JTextField txtField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(txtField, "title");
    assertEquals(txtField, panel.getTextField());
    assertNotNull(panel.getButton());
    panel.setMaxLength(10);
    assertEquals(10, panel.getMaxLength());
    txtField.setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", txtField.getText());
  }

  @Test
  public void constructorNullTextComponent() {
    assertThrows(NullPointerException.class, () -> new TextInputPanel(null, ""));
  }

  @Test
  public void setTextExceedMaxLength() {
    final JTextField txtField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(txtField, "title");
    panel.setMaxLength(5);
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }
}
