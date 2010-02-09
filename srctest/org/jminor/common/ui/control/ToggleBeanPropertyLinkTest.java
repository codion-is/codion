package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import junit.framework.TestCase;

import javax.swing.JCheckBox;

public class ToggleBeanPropertyLinkTest extends TestCase {

  private boolean booleanValue;
  private Event evtBooleanValueChanged = new Event();

  public void test() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    new ToggleBeanPropertyLink(checkBox.getModel(), this, "booleanValue", evtBooleanValueChanged, "");
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
