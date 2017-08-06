/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongFieldTest {

  @Test
  public void test() {
    final LongField txt = new LongField();
    txt.setLong(42L);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Long.valueOf(22), txt.getLong());

    txt.setLong(10000000000000L);
    assertEquals("10000000000000", txt.getText());
    txt.setLong(1000000000000L);
    assertEquals("1000000000000", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());

    txt.setText("");
    txt.setLong(100L);
    assertEquals("", txt.getText());
    txt.setLong(9L);
    assertEquals("9", txt.getText());
    txt.setText("");
    txt.setLong(-1L);
    assertEquals("", txt.getText());
  }
}
