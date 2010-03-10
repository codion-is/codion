package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import javax.swing.JTextField;

public class TextBeanPropertyLinkTest {

  private String stringValue;
  private Event evtStringValueChanged = new Event();

  @Test
  public void test() throws Exception {
    final JTextField txtString = new JTextField();
    new TextBeanPropertyLink(txtString, this, "stringValue", String.class, evtStringValueChanged);
    assertEquals("String value should be empty on initialization", "", txtString.getText());
    setStringValue("hello");
    assertEquals("String value should be 'hello'", "hello", txtString.getText());
    txtString.setText("42");
    assertEquals("String value should be 42", "42", getStringValue());
    txtString.setText("");
    assertEquals("String value should be empty", "", getStringValue());
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }
}
