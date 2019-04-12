/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.DoubleField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DoubleValueLinkTest {

  private Double doubleValue;
  private final Event evtDoubleValueChanged = Events.event();
  private double doublePrimitiveValue;
  private final Event evtDoublePrimitiveValueValueChanged = Events.event();

  @Test
  public void testDouble() throws Exception {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    ValueLinks.doubleValueLink(doubleField, this, "doubleValue", evtDoubleValueChanged, false, true);
    assertNull(doubleField.getDouble());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getDouble());
    doubleField.setText("42.2");
    assertEquals(Double.valueOf(42.2), doubleValue);
    doubleField.setText("");
    assertNull(doubleValue);
  }

  @Test
  public void testDoublePrimitive() throws Exception {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    ValueLinks.doubleValueLink(doubleField, this, "doublePrimitiveValue", evtDoublePrimitiveValueValueChanged, true, true);
    assertEquals((Double) 0.0, doubleField.getDouble());
    setDoublePrimitiveValue(2.2);
    assertEquals(Double.valueOf(2.2), doubleField.getDouble());
    doubleField.setText("42.2");
    assertEquals(42.2, doublePrimitiveValue);
    doubleField.setText("");
    assertEquals(0.0, doublePrimitiveValue);
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
