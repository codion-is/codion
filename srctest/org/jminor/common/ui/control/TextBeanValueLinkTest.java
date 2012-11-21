/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.*;

public class TextBeanValueLinkTest {

  private String stringValue;
  private final Event evtStringValueChanged = Events.event();

  @Test
  public void test() throws Exception {
    final JTextField txtString = new JTextField();
    new TextBeanValueLink(txtString, this, "stringValue", String.class, evtStringValueChanged);
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
    new TextBeanValueLink(txtString2, this, "stringValue", String.class, evtStringValueChanged, LinkType.READ_ONLY);
    assertEquals("test", txtString2.getText());
    assertFalse(txtString2.isEditable());
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }
}
