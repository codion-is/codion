/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.TextInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextValuesTest {

  private String stringValue;
  private final Event<String> stringValueChangedEvent = Events.event();

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    stringValueChangedEvent.onEvent();
  }

  @Test
  public void nullInitialValue() throws Exception {
    stringValue = null;
    final JTextField textField = new JTextField();
    final Value<String> stringPropertyValue = Values.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent);
    TextValues.textValueLink(textField, stringPropertyValue);
    assertNull(this.stringValue);
    assertEquals("", textField.getText());
    setStringValue("hello");
    assertEquals("hello", textField.getText());
    textField.setText("42");
    assertEquals("42", this.stringValue);
    textField.setText("");
    assertNull(this.stringValue);

    final JTextField textField2 = new JTextField();
    this.stringValue = "test";
    TextValues.textValueLink(textField2, Values.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent));
    assertEquals("test", textField2.getText());
  }

  @Test
  public void nonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField textField = new JTextField();
    TextValues.textValueLink(textField, Values.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent));
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
  public void stringValueField() {
    final String value = "hello";
    ComponentValue<String, TextInputPanel> componentValue = TextValues.textValue("none", value, 2);
    assertNull(componentValue.get());

    componentValue = TextValues.textValue("none", value, 10);
    assertEquals(value, componentValue.get());

    componentValue = TextValues.textValue("none", null, 10);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("tester");
    assertEquals("tester", componentValue.get());

    componentValue.getComponent().setText("");
    assertNull(componentValue.get());
  }

  @Test
  public void stringValue() {
    final JTextField textField = new JTextField();
    final Value<String> value = TextValues.textValue(textField);

    assertNull(value.get());
    textField.setText("hello there");
    assertEquals("hello there", value.get());
    textField.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", textField.getText());
  }
}
