/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LongInputProviderTest {

  @Test
  public void test() {
    final Long value = 10L;
    LongInputProvider provider = new LongInputProvider(value);
    assertEquals(value, provider.getValue());

    provider = new LongInputProvider(null);
    assertNull(provider.getValue());

    provider.getInputComponent().setText("15");
    assertEquals(Long.valueOf(15), provider.getValue());

    provider = new LongInputProvider(value, 0, 100);
    assertEquals(value, provider.getValue());
    provider.getInputComponent().setText("");
    provider.getInputComponent().setText("-10");
    assertNull(provider.getValue());
    provider.getInputComponent().setText("150");
    assertNull(provider.getValue());
  }
}