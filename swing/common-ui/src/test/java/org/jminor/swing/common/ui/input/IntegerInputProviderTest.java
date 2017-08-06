/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntegerInputProviderTest {

  @Test
  public void test() {
    final Integer value = 10;
    IntegerInputProvider provider = new IntegerInputProvider(value);
    assertEquals(value, provider.getValue());

    provider = new IntegerInputProvider(null);
    assertNull(provider.getValue());

    provider.getInputComponent().setText("15");
    assertEquals(Integer.valueOf(15), provider.getValue());

    provider = new IntegerInputProvider(value, 0, 100);
    assertEquals(value, provider.getValue());
    provider.getInputComponent().setText("");
    provider.getInputComponent().setText("-10");
    assertNull(provider.getValue());
    provider.getInputComponent().setText("150");
    assertNull(provider.getValue());
  }
}