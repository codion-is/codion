/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.DecimalField;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DoubleValueLinkTest {

  private Double doubleValue;
  private final Event doubleValueChangedEvent = Events.event();
  private double doublePrimitiveValue;
  private final Event doublePrimitiveValueValueChangedEvent = Events.event();

  @Test
  public void testBigDecimal() {
    final DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setParseBigDecimal(true);
    format.setMaximumFractionDigits(4);

    final DecimalField decimalField = new DecimalField(format);
    decimalField.setSeparators('.', ',');

    decimalField.setBigDecimal(BigDecimal.valueOf(3.14));
    assertEquals("3.14", decimalField.getText());

    decimalField.setText("42.4242");
    assertEquals(BigDecimal.valueOf(42.4242), decimalField.getBigDecimal());
  }

  @Test
  public void testDouble() throws Exception {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    ValueLinks.doubleValueLink(decimalField, this, "doubleValue", doubleValueChangedEvent, false, true);
    assertNull(decimalField.getDouble());
    setDoubleValue(2.2);
    assertEquals(Double.valueOf(2.2), decimalField.getDouble());
    decimalField.setText("42.2");
    assertEquals(Double.valueOf(42.2), doubleValue);
    decimalField.setText("");
    assertNull(doubleValue);
  }

  @Test
  public void testDoublePrimitive() throws Exception {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    ValueLinks.doubleValueLink(decimalField, this, "doublePrimitiveValue", doublePrimitiveValueValueChangedEvent, true, true);
    assertEquals((Double) 0.0, decimalField.getDouble());
    setDoublePrimitiveValue(2.2);
    assertEquals(Double.valueOf(2.2), decimalField.getDouble());
    decimalField.setText("42.2");
    assertEquals(42.2, doublePrimitiveValue);
    decimalField.setText("");
    assertEquals(0.0, doublePrimitiveValue);
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(final Double doubleValue) {
    this.doubleValue = doubleValue;
    doubleValueChangedEvent.fire();
  }

  public double getDoublePrimitiveValue() {
    return doublePrimitiveValue;
  }

  public void setDoublePrimitiveValue(final double doublePrimitiveValue) {
    this.doublePrimitiveValue = doublePrimitiveValue;
    doublePrimitiveValueValueChangedEvent.fire();
  }
}
