/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.ui.textfield.DoubleField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class DoubleBeanValueLinkTest {

  private Double doubleValue;
  private Event evtDoubleValueChanged = new Event();

  @Test
  public void test() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    txtDouble.setDecimalSymbol(DoubleField.POINT);
    new DoubleBeanValueLink(txtDouble, this, "doubleValue", evtDoubleValueChanged);
    assertNull("Double value should be null on initialization", txtDouble.getDouble());
    setDoubleValue(2.2);
    assertEquals("Double value should be 2.2", new Double(2.2), txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", new Double(42.2), getDoubleValue());
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
