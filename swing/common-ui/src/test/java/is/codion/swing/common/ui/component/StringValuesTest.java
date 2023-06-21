/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class StringValuesTest {

  private String stringValue;
  private final Event<String> stringValueChangedEvent = Event.event();

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
    stringValueChangedEvent.onEvent();
  }

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
  void nullInitialValue() throws Exception {
    stringValue = null;
    Value<String> stringPropertyValue = Value.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent);
    JTextField textField = Components.textField(stringPropertyValue)
            .build();
    assertNull(this.stringValue);
    assertEquals("", textField.getText());
    setStringValue("hello");
    assertEquals("hello", textField.getText());
    textField.setText("42");
    assertEquals("42", this.stringValue);
    textField.setText("");
    assertNull(this.stringValue);

    this.stringValue = "test";
    JTextField textField2 = Components.textField(Value.propertyValue(this, "stringValue", String.class, stringValueChangedEvent))
            .build();
    assertEquals("test", textField2.getText());
  }

  @Test
  void nonNullInitialValue() throws Exception {
    stringValue = "name";
    JTextField textField = Components.textField(Value.propertyValue(this, "stringValue", String.class, stringValueChangedEvent))
            .build();
    assertEquals("name", textField.getText());
    textField.setText("darri");
    assertFalse(getStringValue().isEmpty());
    assertEquals("darri", getStringValue());
    textField.setText("");
    assertNull(getStringValue());
    setStringValue("Björn");
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
