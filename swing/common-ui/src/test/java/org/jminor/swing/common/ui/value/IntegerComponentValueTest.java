/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.IntegerField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IntegerComponentValueTest {

  @Test
  public void test() {
    final Integer value = 10;
    ComponentValue<Integer, IntegerField> componentValue = ComponentValues.integerValue(value);
    assertEquals(value, componentValue.get());

    componentValue = ComponentValues.integerValue((Integer) null);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("15");
    assertEquals(Integer.valueOf(15), componentValue.get());
  }
}