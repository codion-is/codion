/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.button;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

  @Test
  void iterateState() {
    NullableToggleButtonModel model = new NullableToggleButtonModel();
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
  void setNull() {
    NullableToggleButtonModel model = new NullableToggleButtonModel(true);
    assertTrue(model.getState());
    assertTrue(model.isSelected());
    model.setState(null);
    assertNull(model.getState());
    assertFalse(model.isSelected());
  }

  @Test
  void setSelected() {
    NullableToggleButtonModel model = new NullableToggleButtonModel(false);
    assertFalse(model.getState());
    model.setSelected(true);
    assertTrue(model.isSelected());
    model.setSelected(false);
    assertFalse(model.isSelected());
    model.setState(null);
    assertFalse(model.isSelected());
  }
}
