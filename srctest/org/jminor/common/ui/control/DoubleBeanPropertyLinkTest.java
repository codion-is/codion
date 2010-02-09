package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.ui.textfield.DoubleField;

import junit.framework.TestCase;

public class DoubleBeanPropertyLinkTest extends TestCase {

  private Double doubleValue;
  private Event evtDoubleValueChanged = new Event();

  public void test() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    txtDouble.setDecimalSymbol(DoubleField.POINT);
    new DoubleBeanPropertyLink(txtDouble, this, "doubleValue", evtDoubleValueChanged);
    assertNull("Double value should be null on initialization", txtDouble.getDouble());
    setDoubleValue(2.2);
    assertEquals("Double value should be 2.2", 2.2, txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", 42.2, getDoubleValue());
    txtDouble.setText("");
    assertNull("Double value should be null", getDoubleValue());
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(final Double doubleValue) {
    this.doubleValue = doubleValue;
    evtDoubleValueChanged.fire();
  }
}
