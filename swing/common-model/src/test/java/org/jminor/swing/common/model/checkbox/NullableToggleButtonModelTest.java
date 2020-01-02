/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.checkbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

  @Test
  public void iterateState() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel();
    assertNull(model.getState());
    assertFalse(model.isSelected());
    model.nextState();
    assertFalse(model.getState());
    assertFalse(model.isSelected());
    model.nextState();
    assertTrue(model.getState());
    assertTrue(model.isSelected());
    model.nextState();
    assertNull(model.getState());
  }

  @Test
  public void setNull() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel(true);
    assertTrue(model.getState());
    assertTrue(model.isSelected());
    model.setState(null);
    assertNull(model.getState());
    assertFalse(model.isSelected());
  }

  @Test
  public void setSelected() {
    final NullableToggleButtonModel model = new NullableToggleButtonModel(false);
    assertFalse(model.getState());
    model.setSelected(true);
    assertTrue(model.isSelected());
    model.setSelected(false);
    assertFalse(model.isSelected());
    model.setState(null);
    assertFalse(model.isSelected());
  }
}
