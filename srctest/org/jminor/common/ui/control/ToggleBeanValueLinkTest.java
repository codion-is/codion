/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;

import org.junit.Test;

import javax.swing.JCheckBox;

import static org.junit.Assert.*;

public class ToggleBeanValueLinkTest {

  private boolean booleanValue;
  private final Event evtBooleanValueChanged = Events.event();

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    final ToggleBeanValueLink link = new ToggleBeanValueLink(checkBox.getModel(), this, "booleanValue", evtBooleanValueChanged);
    assertEquals(checkBox.getModel(), link.getButtonModel());
    assertFalse("Boolean value should be false on initialization", checkBox.isSelected());
    setBooleanValue(true);
    assertTrue("Boolean value should be true", checkBox.isSelected());
    checkBox.doClick();
    assertFalse("Boolean value should be false", booleanValue);

    new ToggleBeanValueLink(this, "booleanValue", evtBooleanValueChanged);
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }
}
