/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ItemComboBoxModelTest {

  @Test
  public void test() throws Exception {
    final ItemComboBoxModel.Item[] items = new ItemComboBoxModel.Item[] {
            new ItemComboBoxModel.Item<Integer>(1, "One"),
            new ItemComboBoxModel.Item<Integer>(2, "Two"),
            new ItemComboBoxModel.Item<Integer>(3, "Three"),
            new ItemComboBoxModel.Item<Integer>(4, "Four")};
    final ItemComboBoxModel model = new ItemComboBoxModel(items);

    assertEquals("The item representing 1 should be at index 0", 0, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 1", 1, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 2", 2, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 3", 3, model.getIndexOfItem(4));

    model.setSelectedItem(1);
    assertTrue("The item representing 1 should be selected", model.getSelectedItem().equals(items[0]));
    model.setSelectedItem(2);
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(items[1]));
    model.setSelectedItem(4);
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(items[3]));
  }
}
