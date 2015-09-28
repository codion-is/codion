/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTextComponent() {
    new TextInputPanel(null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setTextExceedMaxLength() {
    final JTextField txtField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(txtField, "title");
    panel.setMaxLength(5);
    panel.setText("123456");
  }
}
