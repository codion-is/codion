/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextFieldHintTest {

  @Test
  public void enableNullTextField() {
    assertThrows(NullPointerException.class, () -> TextFieldHint.enable(null, "test"));
  }

  @Test
  public void enableNullHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.enable(new JTextField(), null));
  }

  @Test
  public void enableEmptyHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.enable(new JTextField(), ""));
  }

  @Test
  public void enableNullForegroundColor() {
    assertThrows(NullPointerException.class, () -> TextFieldHint.enable(new JTextField(), "test", null));
  }

  @Test
  public void test() {
    final JTextField textField = new JTextField();
    final TextFieldHint hint = TextFieldHint.enable(textField, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", textField.getText());
    textField.setText("he");
    assertEquals("he", textField.getText());
  }
}
