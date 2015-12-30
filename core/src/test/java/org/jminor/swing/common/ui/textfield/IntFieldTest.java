/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntFieldTest {

  @Test
  public void test() {
    final IntField txt = new IntField();
    txt.setInt(42);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Integer.valueOf(22), txt.getInt());

    txt.setInt(10000000);
    assertEquals("10000000", txt.getText());
    txt.setInt(100000000);
    assertEquals("100000000", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());

    txt.setInt(100);
    assertEquals("", txt.getText());
    txt.setInt(9);
    assertEquals("9", txt.getText());
    txt.setInt(-1);
    assertEquals("", txt.getText());
    txt.setInt(-10);
    assertEquals("", txt.getText());
  }
}
