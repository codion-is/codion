/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;

public class TextFieldHintTest {

  @Test(expected = IllegalArgumentException.class)
  public void enableNullTextField() {
    TextFieldHint.enable(null, "test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void enableNullHintString() {
    TextFieldHint.enable(new JTextField(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void enableEmptyHintString() {
    TextFieldHint.enable(new JTextField(), "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void enableNullForegroundColor() {
    TextFieldHint.enable(new JTextField(), "test", null);
  }

  @Test
  public void test() {
    final JTextField txt = new JTextField();
    final TextFieldHint hint = TextFieldHint.enable(txt, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", txt.getText());
    txt.setText("he");
    assertEquals("he", txt.getText());
  }
}
