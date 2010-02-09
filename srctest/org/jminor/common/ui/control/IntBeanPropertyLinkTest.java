package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.ui.textfield.IntField;

import junit.framework.TestCase;

public class IntBeanPropertyLinkTest extends TestCase {

  private Integer intValue;
  private Event evtIntValueChanged = new Event();

  public void test() throws Exception {
    final IntField txtInt = new IntField();
    new IntBeanPropertyLink(txtInt, this, "intValue", evtIntValueChanged);
    assertNull("Int value should be null on initialization", txtInt.getInt());
    setIntValue(2);
    assertEquals("Int value should be 2", 2, txtInt.getInt().intValue());
    txtInt.setText("42");
    assertEquals("Int value should be 42", 42, getIntValue().intValue());
    txtInt.setText("");
    assertNull("Int value should be null", getIntValue());
  }

  public Integer getIntValue() {
    return intValue;
  }

  public void setIntValue(final Integer intValue) {
    this.intValue = intValue;
    evtIntValueChanged.fire();
  }
}
