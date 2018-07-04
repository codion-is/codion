/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SelectedItemValueLinkTest {

  private String selectedItem;
  private final Event evtSelectedItemChanged = Events.event();

  @Test
  public void test() throws Exception {
    final JComboBox box = new JComboBox(new String[] {"b", "d", "s"});
    ValueLinks.selectedItemValueLink(box, this, "selectedItem", String.class, evtSelectedItemChanged);
    assertNull(selectedItem);
    setSelectedItem("s");
    assertEquals("s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("d", selectedItem);
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(final String selectedItem) {
    this.selectedItem = selectedItem;
    evtSelectedItemChanged.fire();
  }
}
