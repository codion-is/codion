/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LongFieldTest {

  @Test
  public void test() {
    final LongField txt = new LongField();
    txt.setLong(42l);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Long.valueOf(22), txt.getLong());

    txt.setLong(10000000000000l);
    assertEquals("10000000000000", txt.getText());
    txt.setLong(1000000000000l);
    assertEquals("1000000000000", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());

    txt.setLong(100l);
    assertEquals("", txt.getText());
    txt.setLong(9l);
    assertEquals("9", txt.getText());
    txt.setLong(-1l);
    assertEquals("", txt.getText());
  }
}
