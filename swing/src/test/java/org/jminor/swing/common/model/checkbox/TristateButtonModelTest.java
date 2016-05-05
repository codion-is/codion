/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.checkbox;

import org.junit.Test;

import static org.junit.Assert.*;

public class TristateButtonModelTest {

  @Test
  public void iterateState() {
    final TristateButtonModel model = new TristateButtonModel();
    assertEquals(TristateState.DESELECTED, model.getState());
    model.iterateState();
    assertEquals(TristateState.SELECTED, model.getState());
    model.iterateState();
    assertEquals(TristateState.INDETERMINATE, model.getState());
    model.iterateState();
    assertEquals(TristateState.DESELECTED, model.getState());
  }

  @Test
  public void setIndeterminate() {
    final TristateButtonModel model = new TristateButtonModel();
    model.setIndeterminate();
    assertTrue(model.isIndeterminate());
    assertEquals(TristateState.INDETERMINATE, model.getState());
  }

  @Test
  public void setSelected() {
    final TristateButtonModel model = new TristateButtonModel();
    assertEquals(TristateState.DESELECTED, model.getState());
    model.setSelected(true);
    assertTrue(model.isSelected());
    model.setSelected(false);
    assertFalse(model.isSelected());
    model.setArmed(false);
    model.setPressed(false);
  }
}
