/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TextFieldsTest {

  @Test
  void upperCase() {
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
  void lowerCase() {
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
  void selectAllOnFocusGained() {
    final JTextField textField = new JTextField("test");
    final int focusListenerCount = textField.getFocusListeners().length;
    TextFields.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    TextFields.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }

  @Test
  void enableNullTextField() {
    assertThrows(NullPointerException.class, () -> TextFieldHint.create(null, "test"));
  }

  @Test
  void enableNullHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.create(new JTextField(), null));
  }

  @Test
  void enableEmptyHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.create(new JTextField(), ""));
  }

  @Test
  void hint() {
    final JTextField textField = new JTextField();
    final TextFieldHint hint = TextFieldHint.create(textField, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", textField.getText());
    textField.setText("he");
    assertEquals("he", textField.getText());
  }
}
