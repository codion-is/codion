/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import javax.swing.JCheckBox;

public class ToggleBeanPropertyLinkTest {

  private boolean booleanValue;
  private Event evtBooleanValueChanged = new Event();

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    final ToggleBeanPropertyLink link = new ToggleBeanPropertyLink(checkBox.getModel(), this, "booleanValue", evtBooleanValueChanged, "");
    assertEquals(checkBox.getModel(), link.getButtonModel());
    assertFalse("Boolean value should be false on initialization", checkBox.isSelected());
    setBooleanValue(true);
    assertTrue("Boolean value should be true", checkBox.isSelected());
    checkBox.doClick();
    assertFalse("Boolean value should be false", isBooleanValue());
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }
}
