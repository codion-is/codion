/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.Test;

import javax.swing.JCheckBox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToggleValueLinkTest {

  private boolean booleanValue;
  private final Event evtBooleanValueChanged = Events.event();

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(), this, "booleanValue", evtBooleanValueChanged);
    assertFalse("Boolean value should be false on initialization", checkBox.isSelected());
    setBooleanValue(true);
    assertTrue("Boolean value should be true", checkBox.isSelected());
    checkBox.doClick();
    assertFalse("Boolean value should be false", booleanValue);
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }
}
