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
    ComponentValue<Double, DecimalField> provider = DoubleValues.doubleValue(value);
    assertEquals(value, provider.get());
    provider = DoubleValues.doubleValue((Double) null);
    assertNull(provider.get());
  }

  @Test
  public void parse() {
    final ComponentValue<Double, DecimalField> componentValue = DoubleValues.doubleValue((Double) null);
    assertNull(componentValue.get());

    componentValue.getComponent().setGroupingUsed(false);

    componentValue.getComponent().setSeparators('.', ',');
    componentValue.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,6");
    assertEquals(Double.valueOf(15.5), componentValue.get());

    componentValue.getComponent().setSeparators(',', '.');
    componentValue.getComponent().setText("15.7");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());

    componentValue.getComponent().setGroupingUsed(true);

    componentValue.getComponent().setSeparators('.', ',');
    componentValue.getComponent().setText("15.5");
    assertEquals(Double.valueOf(15.5), componentValue.get());
    componentValue.getComponent().setText("15,6");
    assertEquals(Double.valueOf(156), componentValue.get());

    componentValue.getComponent().setSeparators(',', '.');
    componentValue.getComponent().setText("15.7");
    assertEquals(Double.valueOf(157), componentValue.get());
    componentValue.getComponent().setText("15,7");
    assertEquals(Double.valueOf(15.7), componentValue.get());
  }
}