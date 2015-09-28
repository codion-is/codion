/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LongInputProviderTest {

  @Test
  public void test() {
    final Long value = 10l;
    LongInputProvider provider = new LongInputProvider(value);
    assertEquals(value, provider.getValue());

    provider = new LongInputProvider(null);
    assertNull(provider.getValue());

    provider.getInputComponent().setText("15");
    assertEquals(Long.valueOf(15), provider.getValue());

    provider = new LongInputProvider(value, 0, 100);
    assertEquals(value, provider.getValue());
    provider.getInputComponent().setText("-10");
    assertNull(provider.getValue());
    provider.getInputComponent().setText("150");
    assertNull(provider.getValue());
  }
}