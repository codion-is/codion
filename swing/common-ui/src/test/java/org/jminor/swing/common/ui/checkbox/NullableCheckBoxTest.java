/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.checkbox;

import org.jminor.swing.common.model.checkbox.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableCheckBoxTest {

  @Test
  public void test() {
    final NullableCheckBox box = new NullableCheckBox("Test", new NullableToggleButtonModel(false));
    assertFalse(box.get());
    box.getMouseListeners()[0].mousePressed(null);
    assertTrue(box.get());
    box.getMouseListeners()[0].mousePressed(null);
    assertNull(box.get());
    box.getMouseListeners()[0].mousePressed(null);
    assertFalse(box.get());

    box.getNullableModel().setEnabled(false);
    box.getMouseListeners()[0].mousePressed(null);
    assertTrue(box.get());

    box.getNullableModel().set(null);
    assertNull(box.get());
  }
}
