/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.checkbox;

import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableCheckBoxTest {

  @Test
  public void test() {
    final NullableCheckBox box = new NullableCheckBox(new NullableToggleButtonModel(false), "Test");
    assertFalse(box.getState());
    box.getMouseListeners()[0].mouseClicked(null);
    assertTrue(box.getState());
    box.getMouseListeners()[0].mouseClicked(null);
    assertNull(box.getState());
    box.getMouseListeners()[0].mouseClicked(null);
    assertFalse(box.getState());

    box.getModel().setEnabled(false);
    box.getMouseListeners()[0].mouseClicked(null);
    assertTrue(box.getState());

    box.getNullableModel().setState(null);
    assertNull(box.getState());

    assertThrows(UnsupportedOperationException.class, () -> box.setModel(new NullableToggleButtonModel()));
  }
}
