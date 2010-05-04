/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JTextField;

public class TextBeanPropertyLinkTest {

  private String stringValue;
  private Event evtStringValueChanged = new Event();

  @Test
  public void test() throws Exception {
    final JTextField txtString = new JTextField();
    new TextBeanValueLink(txtString, this, "stringValue", String.class, evtStringValueChanged);
    assertNull("String value should be null", getStringValue());
    assertEquals("String value should be empty on initialization", "", txtString.getText());
    setStringValue("hello");
    assertEquals("String value should be 'hello'", "hello", txtString.getText());
    txtString.setText("42");
    assertEquals("String value should be 42", "42", getStringValue());
    txtString.setText("");
    assertNull("String value should be null", getStringValue());
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }
}
