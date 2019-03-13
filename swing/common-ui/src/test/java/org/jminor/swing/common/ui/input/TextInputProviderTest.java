/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TextInputProviderTest {

  @Test
  public void test() {
    final String value = "hello";
    TextInputProvider provider = new TextInputProvider("none", null, value, 2);
    assertNull(provider.getValue());

    provider = new TextInputProvider("none", null, value, 10);
    assertEquals(value, provider.getValue());

    provider = new TextInputProvider("none", null, null, 10);
    assertNull(provider.getValue());

    provider.getInputComponent().setText("tester");
    assertEquals("tester", provider.getValue());

    provider.getInputComponent().setText("");
    assertNull(provider.getValue());
  }
}