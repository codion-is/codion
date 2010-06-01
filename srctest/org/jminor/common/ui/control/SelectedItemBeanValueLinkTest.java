/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JComboBox;

/**
 * User: Bjorn Darri
 * Date: 13.1.2008
 * Time: 12:15:13
 */
public class SelectedItemBeanValueLinkTest {

  private String selectedItem;
  private Event evtSelectedItemChanged = new Event();

  @Test
  public void test() throws Exception {
    final JComboBox box = new JComboBox(new String[] {"b", "d", "s"});
    new SelectedItemBeanValueLink(box, this, "selectedItem", String.class, evtSelectedItemChanged);
    assertNull("selected item should be null", getSelectedItem());
    setSelectedItem("s");
    assertEquals("selected item should be 's'", "s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("selected item should be 'd'", "d", getSelectedItem());
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(final String selectedItem) {
    this.selectedItem = selectedItem;
    evtSelectedItemChanged.fire();
  }
}
