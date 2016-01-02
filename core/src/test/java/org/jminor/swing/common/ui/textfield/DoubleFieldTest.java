/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import javax.swing.text.BadLocationException;

import static org.junit.Assert.assertEquals;

public class DoubleFieldTest {

  @Test
  public void testNoGrouping() {
    final DoubleField txt = new DoubleField();
    txt.setSeparators(',', '.');
    txt.setText(",");
    assertEquals("0,", txt.getText());
    txt.setDouble(42.2);
    assertEquals("42,2", txt.getText());
    txt.setText("22,3");
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    txt.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), txt.getDouble());
    assertEquals("223", txt.getText());
    txt.setText("22.123.123,123");
    assertEquals("22123123,123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setSeparators('.', ',');

    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());
    txt.setText("22,123,123.123");
    assertEquals("22123123.123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setDouble(10000000d);
    assertEquals("10000000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100000000.4", txt.getText());
  }

  @Test
  public void testGrouping() {
    final DoubleField txt = new DoubleField();
    txt.setGroupingUsed(true);
    txt.setSeparators(',', '.');
    txt.setText(",");
    assertEquals("0,", txt.getText());
    txt.setDouble(42.2);
    assertEquals("42,2", txt.getText());
    txt.setText("22,3");
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    txt.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), txt.getDouble());
    assertEquals("22.3", txt.getText());
    txt.setText("22.123.123,123");
    assertEquals("22.123.123,123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setSeparators('.', ',');

    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());
    txt.setText("22,123,123.123");
    assertEquals("22,123,123.123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setDouble(10000000d);
    assertEquals("10,000,000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100,000,000.4", txt.getText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaximumFractionDigitsToZero() {
    new DoubleField().setMaximumFractionDigits(0);
  }

  @Test
  public void maximumFractionDigits() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    assertEquals(-1, txt.getMaximumFractionDigits());
    txt.setSeparators(',', '.');
    txt.setMaximumFractionDigits(2);
    assertEquals(2, txt.getMaximumFractionDigits());
    txt.setDouble(5.1254);
    assertEquals("5,13", txt.getText());
    txt.setText("5,123");
    assertEquals("5,12", txt.getText());
    txt.getDocument().insertString(3, "4", null);
    assertEquals("5,14", txt.getText());
    txt.setMaximumFractionDigits(3);
    txt.setText("5,12378");
    assertEquals("5,123", txt.getText());//no rounding should occur
    txt.setMaximumFractionDigits(-1);
    txt.setText("5,12378");
    assertEquals("5,12378", txt.getText());
  }
}
