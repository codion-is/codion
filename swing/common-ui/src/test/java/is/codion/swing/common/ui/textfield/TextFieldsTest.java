/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextFieldsTest {

  @Test
  public void upperCase() {
    JTextField textField = TextFields.upperCase(new JTextField());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.upperCase(textField);
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());
  }

  @Test
  public void lowerCase() {
    JTextField textField = TextFields.lowerCase(new JTextField());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.lowerCase(textField);
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
