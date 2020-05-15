/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextFieldsTest {

  @Test
  public void makeUpperCase() {
    JTextField textField = TextFields.makeUpperCase(new JTextField());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.makeUpperCase(textField);
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());
  }

  @Test
  public void makeLowerCase() {
    JTextField textField = TextFields.makeLowerCase(new JTextField());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.makeLowerCase(textField);
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());
  }

  @Test
  public void selectAllOnFocusGained() {
    final JTextField textField = new JTextField("test");
    final int focusListenerCount = textField.getFocusListeners().length;
    TextFields.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    TextFields.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }
}
