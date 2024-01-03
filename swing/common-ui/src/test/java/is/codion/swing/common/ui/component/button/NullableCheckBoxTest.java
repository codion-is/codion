/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseListener;

import static org.junit.jupiter.api.Assertions.*;

public class NullableCheckBoxTest {

  @Test
  void test() {
    NullableCheckBox box = new NullableCheckBox(new NullableToggleButtonModel(false), "Test");
    assertFalse(box.getState());
    MouseListener mouseListener = box.getMouseListeners()[1];
    mouseListener.mouseClicked(null);
    assertTrue(box.getState());
    mouseListener.mouseClicked(null);
    assertNull(box.getState());
    mouseListener.mouseClicked(null);
    assertFalse(box.getState());

    box.getModel().setEnabled(false);
    mouseListener.mouseClicked(null);
    assertFalse(box.getState());

    box.getNullableModel().setState(null);
    assertNull(box.getState());

    assertThrows(UnsupportedOperationException.class, () -> box.setModel(new NullableToggleButtonModel()));
  }
}
