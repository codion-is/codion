/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToggleValueLinkTest {

  private boolean booleanValue;
  private final Event evtBooleanValueChanged = Events.event();

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(), this, "booleanValue", evtBooleanValueChanged);
    assertFalse(checkBox.isSelected());
    setBooleanValue(true);
    assertTrue(checkBox.isSelected());
    checkBox.doClick();
    assertFalse(booleanValue);
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }
}
