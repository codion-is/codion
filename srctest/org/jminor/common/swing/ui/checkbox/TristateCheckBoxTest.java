/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.checkbox;

import org.jminor.common.swing.model.checkbox.TristateState;

import org.junit.Test;

import static org.junit.Assert.*;

public class TristateCheckBoxTest {

  @Test
  public void test() {
    final TristateCheckBox box = new TristateCheckBox("Test");
    assertEquals(TristateState.DESELECTED, box.getState());
    assertFalse(box.isIndeterminate());
    box.getMouseListeners()[0].mousePressed(null);
    assertEquals(TristateState.SELECTED, box.getState());
    assertFalse(box.isIndeterminate());
    box.getMouseListeners()[0].mousePressed(null);
    assertEquals(TristateState.INDETERMINATE, box.getState());
    assertTrue(box.isIndeterminate());
    box.getMouseListeners()[0].mousePressed(null);
    assertEquals(TristateState.DESELECTED, box.getState());
    assertFalse(box.isIndeterminate());

    box.getTristateModel().setEnabled(false);
    box.getMouseListeners()[0].mousePressed(null);
    assertEquals(TristateState.DESELECTED, box.getState());
    assertFalse(box.isIndeterminate());

    box.setIndeterminate();
    assertTrue(box.isIndeterminate());
  }
}
