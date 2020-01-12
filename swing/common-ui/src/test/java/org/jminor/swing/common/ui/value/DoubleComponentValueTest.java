/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.DecimalField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DoubleComponentValueTest {

  @Test
  public void constructor() {
    final Double value = 10.4;
    ComponentValue<Double, DecimalField> provider = ComponentValues.doubleValue(value);
    assertEquals(value, provider.get());
    provider = ComponentValues.doubleValue((Double) null);
    assertNull(provider.get());
  }

  @Test
  public void parse() {
    final ComponentValue<Double, DecimalField> provider = ComponentValues.doubleValue((Double) null);
    assertNull(provider.get());

    provider.getComponent().setGroupingUsed(false);

    provider.getComponent().setSeparators('.', ',');
    provider.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), provider.get());
    provider.getComponent().setText("15,6");
    assertEquals(Double.valueOf(15.5), provider.get());

    provider.getComponent().setSeparators(',', '.');
    provider.getComponent().setText("15.7");
    assertEquals(Double.valueOf(15.5), provider.get());
    provider.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), provider.get());

    provider.getComponent().setGroupingUsed(true);

    provider.getComponent().setSeparators('.', ',');
    provider.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), provider.get());
    provider.getComponent().setText("15,6");
    assertEquals(Double.valueOf(156), provider.get());

    provider.getComponent().setSeparators(',', '.');
    provider.getComponent().setText("15.7");
    assertEquals(Double.valueOf(157), provider.get());
    provider.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), provider.get());
  }
}