/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.ui.images.Images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import javax.swing.ImageIcon;

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
    assertEquals("The item representing 1 should be selected", "One", model.getSelectedItem().toString());
    model.setSelectedItem(2);
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(items[1]));
    model.setSelectedItem(4);
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(items[3]));

    final ImageIcon icon = Images.loadImage("Equals60x16.gif");
    final ItemComboBoxModel iconModel = new ItemComboBoxModel(new ItemComboBoxModel.IconItem("test", icon));
    iconModel.setSelectedItem("test");
    final ItemComboBoxModel.IconItem item = (ItemComboBoxModel.IconItem) iconModel.getSelectedItem();
    assertEquals(icon.getIconHeight(), item.getIconHeight());
    assertEquals(icon.getIconWidth(), item.getIconWidth());
    assertEquals("test", item.toString());
  }
}
