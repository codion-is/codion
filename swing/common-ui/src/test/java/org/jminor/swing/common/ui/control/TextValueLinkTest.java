/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
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
  private final Event<String> evtStringValueChanged = Events.event();

  @Test
  public void testNullInitialValue() throws Exception {
    stringValue = null;
    final JTextField txtString = new JTextField();
    ValueLinks.textValueLink(txtString, this, "stringValue", evtStringValueChanged);
    assertNull(stringValue);
    assertEquals("", txtString.getText());
    setStringValue("hello");
    assertEquals("hello", txtString.getText());
    txtString.setText("42");
    assertEquals("42", stringValue);
    txtString.setText("");
    assertNull(stringValue);

    final JTextField txtString2 = new JTextField();
    stringValue = "test";
    ValueLinks.textValueLink(txtString2, this, "stringValue", evtStringValueChanged, true);
    assertEquals("test", txtString2.getText());
    assertFalse(txtString2.isEditable());
  }

  @Test
  public void testNonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField txtString = new JTextField();
    ValueLinks.textValueLink(txtString, this, "stringValue", evtStringValueChanged);
    assertEquals("name", txtString.getText());
    txtString.setText("darri");
    assertFalse(getStringValue().isEmpty());
    assertEquals("darri", getStringValue());
    txtString.setText("");
    assertNull(getStringValue());
    setStringValue("Björn");
    assertEquals("Björn", txtString.getText());
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }
}
