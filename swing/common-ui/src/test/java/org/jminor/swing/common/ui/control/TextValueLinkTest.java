/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;

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
    ValueLinks.textValueLink(textField, this, "stringValue", stringValueChangedEvent);
    assertNull(stringValue);
    assertEquals("", textField.getText());
    setStringValue("hello");
    assertEquals("hello", textField.getText());
    textField.setText("42");
    assertEquals("42", stringValue);
    textField.setText("");
    assertNull(stringValue);

    final JTextField textField2 = new JTextField();
    stringValue = "test";
    ValueLinks.textValueLink(textField2, this, "stringValue", stringValueChangedEvent, true);
    assertEquals("test", textField2.getText());
    assertFalse(textField2.isEditable());
  }

  @Test
  public void testNonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField textField = new JTextField();
    ValueLinks.textValueLink(textField, this, "stringValue", stringValueChangedEvent);
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
    stringValueChangedEvent.fire();
  }
}
