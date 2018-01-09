/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BooleanInputProviderTest {

  @Test
  public void test() {
    BooleanInputProvider provider = new BooleanInputProvider(false);
    assertEquals(false, provider.getValue());
    provider.getInputComponent().getModel().setSelectedItem(true);
    assertEquals(true, provider.getValue());
    provider.getInputComponent().getModel().setSelectedItem(null);
    assertNull(provider.getValue());
    provider = new BooleanInputProvider(null);
    assertNull(provider.getValue());
  }
}
