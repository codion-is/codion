/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.checkbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

  @Test
  public void iterateState() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel();
    assertNull(model.get());
    assertFalse(model.isSelected());
    model.nextState();
    assertFalse(model.get());
    assertFalse(model.isSelected());
    model.nextState();
    assertTrue(model.get());
    assertTrue(model.isSelected());
    model.nextState();
    assertNull(model.get());
  }

  @Test
  public void setNull() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel(true);
    assertTrue(model.get());
    assertTrue(model.isSelected());
    model.set(null);
    assertNull(model.get());
    assertFalse(model.isSelected());
  }

  @Test
  public void setSelected() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel(false);
    assertFalse(model.get());
    model.setSelected(true);
    assertTrue(model.isSelected());
    model.setSelected(false);
    assertFalse(model.isSelected());
    model.set(null);
    assertFalse(model.isSelected());
  }
}
