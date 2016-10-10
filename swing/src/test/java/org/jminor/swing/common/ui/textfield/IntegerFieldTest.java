/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntegerFieldTest {

  @Test
  public void test() {
    final IntegerField txt = new IntegerField();
    txt.setInteger(42);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Integer.valueOf(22), txt.getInteger());

    txt.setInteger(10000000);
    assertEquals("10000000", txt.getText());
    txt.setInteger(100000000);
    assertEquals("100000000", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());

    txt.setInteger(100);
    assertEquals("", txt.getText());
    txt.setInteger(9);
    assertEquals("9", txt.getText());
    txt.setInteger(-1);
    assertEquals("", txt.getText());
    txt.setInteger(-10);
    assertEquals("", txt.getText());
  }
}
