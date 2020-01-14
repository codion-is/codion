/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class TextValueLinkTest {

  private String stringValue;
  private final Event<String> stringValueChangedEvent = Events.event();

  @Test
  public void testNullInitialValue() throws Exception {
    stringValue = null;
    final JTextField textField = new JTextField();
    final Value<String> stringPropertyValue = Values.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent);
    StringValues.stringValueLink(textField, stringPropertyValue);
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
    StringValues.stringValueLink(textField2, Values.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent));
    assertEquals("test", textField2.getText());
  }

  @Test
  public void testNonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField textField = new JTextField();
    StringValues.stringValueLink(textField, Values.propertyValue(this, "stringValue",
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

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    stringValueChangedEvent.onEvent();
  }
}
