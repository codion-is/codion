/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.*;

public class TextValueLinkTest {

  private String stringValue;
  private final Event evtStringValueChanged = Events.event();

  @Test
  public void testNullInitialValue() throws Exception {
    stringValue = null;
    final JTextField txtString = new JTextField();
    ValueLinks.textBeanValueLink(txtString, this, "stringValue", String.class, evtStringValueChanged);
    assertNull("String value should be null", stringValue);
    assertEquals("String value should be empty on initialization", "", txtString.getText());
    setStringValue("hello");
    assertEquals("String value should be 'hello'", "hello", txtString.getText());
    txtString.setText("42");
    assertEquals("String value should be 42", "42", stringValue);
    txtString.setText("");
    assertNull("String value should be null", stringValue);

    final JTextField txtString2 = new JTextField();
    stringValue = "test";
    ValueLinks.textBeanValueLink(txtString2, this, "stringValue", String.class, evtStringValueChanged, LinkType.READ_ONLY);
    assertEquals("test", txtString2.getText());
    assertFalse(txtString2.isEditable());
  }

  @Test
  public void testNonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField txtString = new JTextField();
    ValueLinks.textBeanValueLink(txtString, this, "stringValue", String.class, evtStringValueChanged);
    assertEquals("name", txtString.getText());
    txtString.setText("darri");
    assertFalse("String value should not be empty", getStringValue().isEmpty());
    assertEquals("String value should be 'darri", "darri", getStringValue());
    txtString.setText("");
    assertTrue("String value should be null", getStringValue() == null);
    setStringValue("Björn");
    assertEquals("Text field should contain value", "Björn", txtString.getText());
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }
}
