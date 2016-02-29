/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.DoubleField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DoubleValueLinkTest {

  private Double doubleValue;
  private final Event evtDoubleValueChanged = Events.event();
  private double doublePrimitiveValue;
  private final Event evtDoublePrimitiveValueValueChanged = Events.event();

  @Test
  public void testDouble() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    txtDouble.setSeparators('.', ',');
    ValueLinks.doubleValueLink(txtDouble, this, "doubleValue", evtDoubleValueChanged, false, true);
    assertNull("Double value should be null on initialization", txtDouble.getDouble());
    setDoubleValue(2.2);
    assertEquals("Double value should be 2.2", new Double(2.2), txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", new Double(42.2), doubleValue);
    txtDouble.setText("");
    assertNull("Double value should be null", doubleValue);
  }

  @Test
  public void testDoublePrimitive() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    txtDouble.setSeparators('.', ',');
    ValueLinks.doubleValueLink(txtDouble, this, "doublePrimitiveValue", evtDoublePrimitiveValueValueChanged, true, true);
    assertEquals("Double value should be 0 on initialization", (Double) 0.0, txtDouble.getDouble());
    setDoublePrimitiveValue(2.2);
    assertEquals("Double value should be 2.2", new Double(2.2), txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", 42.2, 0, doublePrimitiveValue);
    txtDouble.setText("");
    assertEquals("Double value should be 0", 0.0, 0, doublePrimitiveValue);
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(final Double doubleValue) {
    this.doubleValue = doubleValue;
    evtDoubleValueChanged.fire();
  }

  public double getDoublePrimitiveValue() {
    return doublePrimitiveValue;
  }

  public void setDoublePrimitiveValue(final double doublePrimitiveValue) {
    this.doublePrimitiveValue = doublePrimitiveValue;
    evtDoublePrimitiveValueValueChanged.fire();
  }
}
