/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToggleValueLinkTest {

  private boolean booleanValue;
  private final Event booleanValueChangedEvent = Events.event();

  @Test
  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(), this, "booleanValue", booleanValueChangedEvent);
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
    booleanValueChangedEvent.onEvent();
  }
}
