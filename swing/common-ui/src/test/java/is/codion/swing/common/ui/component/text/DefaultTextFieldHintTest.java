/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultTextFieldHintTest {

  @Test
  void nullTextField() {
    assertThrows(NullPointerException.class, () -> new DefaultTextFieldHint(null, "test"));
  }

  @Test
  void nullHintString() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultTextFieldHint(new JTextField(), null));
  }

  @Test
  void emptyHintString() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultTextFieldHint(new JTextField(), ""));
  }

  @Test
  void hint() {
    JTextField textField = new JTextField();
    TextFieldHint hint = new DefaultTextFieldHint(textField, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", textField.getText());
    textField.setText("he");
    assertEquals("he", textField.getText());

    assertThrows(IllegalStateException.class, () -> new DefaultTextFieldHint(textField, "test"));
  }
}
