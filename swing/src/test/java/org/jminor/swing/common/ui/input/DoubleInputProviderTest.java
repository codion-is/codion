/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.textfield.DoubleField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DoubleInputProviderTest {

  @Test
  public void constructor() {
    final Double value = 10.4;
    final DoubleInputProvider provider = new DoubleInputProvider(value);
    assertEquals(value, provider.getValue());
  }

  @Test
  public void parse() {
    final DoubleInputProvider provider = new DoubleInputProvider(null);
    assertNull(provider.getValue());

    ((DoubleField) provider.getInputComponent()).setSeparators('.', ',');
    provider.getInputComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), provider.getValue());
    provider.getInputComponent().setText("15,5");
    assertEquals(Double.valueOf(155), provider.getValue());

    ((DoubleField) provider.getInputComponent()).setSeparators(',', '.');
    provider.getInputComponent().setText("15.5");
    assertEquals(Double.valueOf(155), provider.getValue());
    provider.getInputComponent().setText("15,5");
    assertEquals(Double.valueOf(15.5), provider.getValue());
  }

  @Test
  public void testRange() {
    final Double value = 10.4;
    final DoubleInputProvider provider = new DoubleInputProvider(value, 0, 100);
    assertEquals(value, provider.getValue());
    provider.getInputComponent().setText("");
    provider.getInputComponent().setText("-10");
    assertNull(provider.getValue());
    provider.getInputComponent().setText("150");
    assertNull(provider.getValue());
  }
}