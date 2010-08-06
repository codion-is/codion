/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;
import org.jminor.common.ui.images.Images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import javax.swing.ImageIcon;
import java.util.Arrays;
import java.util.List;

public class ItemComboBoxModelTest {

  @Test
  public void test() throws Exception {
    new ItemComboBoxModel();
    final List<Item<Integer>> items = Arrays.asList(
            new Item<Integer>(1, "AOne"),
            new Item<Integer>(2, "BTwo"),
            new Item<Integer>(3, "CThree"),
            new Item<Integer>(4, "DFour"));
    final ItemComboBoxModel model = new ItemComboBoxModel<Integer>(items);

    assertEquals("The item representing 1 should be at index 0", 0, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 1", 1, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 2", 2, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 3", 3, model.getIndexOfItem(4));

    model.setSelectedItem(1);
    assertTrue("The item representing 1 should be selected", model.getSelectedItem().equals(items.get(0)));
    assertEquals("The item representing 1 should be selected", "AOne", model.getSelectedItem().toString());
    model.setSelectedItem(2);
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(items.get(1)));
    model.setSelectedItem(4);
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(items.get(3)));

    final ImageIcon icon = Images.loadImage("Equals60x16.gif");
    final ItemComboBoxModel<String> iconModel = new ItemComboBoxModel<String>(new ItemComboBoxModel.IconItem<String>("test", icon));
    iconModel.setSelectedItem("test");
    final ItemComboBoxModel.IconItem item = (ItemComboBoxModel.IconItem) iconModel.getSelectedItem();
    assertEquals(icon.getIconHeight(), item.getIconHeight());
    assertEquals(icon.getIconWidth(), item.getIconWidth());
    assertEquals("test", item.toString());
  }
}
