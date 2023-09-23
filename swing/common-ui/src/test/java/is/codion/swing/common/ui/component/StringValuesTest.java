/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class StringValuesTest {

  @Test
  void valueLink() {
    Value<String> textValue = Value.value("start");
    textValue.addValidator(text -> {
      if (text != null && text.equals("nono")) {
        throw new IllegalArgumentException();
      }
    });
    ComponentValue<String, JTextField> textFieldValue = Components.textField(textValue)
            .buildValue();

    assertEquals("start", textFieldValue.get());

    textFieldValue.set("testing");
    assertEquals("testing", textValue.get());

    assertThrows(IllegalArgumentException.class, () -> textFieldValue.set("nono"));
    assertEquals("testing", textFieldValue.get());

    textValue.set("hello");
    assertEquals("hello", textFieldValue.get());

    assertThrows(IllegalArgumentException.class, () -> textValue.set("nono"));
  }

  @Test
  void nullInitialValue() {
    Value<String> stringPropertyValue = Value.value();
    JTextField textField = Components.textField(stringPropertyValue)
            .build();
    assertNull(stringPropertyValue.get());
    assertEquals("", textField.getText());
    stringPropertyValue.set("hello");
    assertEquals("hello", textField.getText());
    textField.setText("42");
    assertEquals("42", stringPropertyValue.get());
    textField.setText("");
    assertNull(stringPropertyValue.get());

    stringPropertyValue.set("test");
    JTextField textField2 = Components.textField(stringPropertyValue)
            .build();
    assertEquals("test", textField2.getText());
  }

  @Test
  void nonNullInitialValue() {
    Value<String> value = Value.value("name");
    JTextField textField = Components.textField(value)
            .build();
    assertEquals("name", textField.getText());
    textField.setText("darri");
    assertFalse(value.get().isEmpty());
    assertEquals("darri", value.get());
    textField.setText("");
    assertNull(value.get());
    value.set("Björn");
    assertEquals("Björn", textField.getText());
  }

  @Test
  void textValue() {
    ComponentValue<String, JTextField> value = Components.textField()
            .buildValue();
    JTextField textField = value.component();

    assertNull(value.get());
    textField.setText("hello there");
    assertEquals("hello there", value.get());
    textField.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", textField.getText());
  }

  @Test
  void characterValue() {
    ComponentValue<Character, JTextField> value = Components.textField(Character.class)
            .buildValue();
    assertNull(value.get());
    value.component().setText("2");
    assertEquals('2', value.get());
    value.set(null);
    assertTrue(value.component().getText().isEmpty());
  }
}
