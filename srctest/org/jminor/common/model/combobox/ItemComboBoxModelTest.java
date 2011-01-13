/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.combobox;

import org.jminor.common.model.Item;
import org.jminor.common.ui.images.Images;

import org.junit.Test;

import javax.swing.ImageIcon;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ItemComboBoxModelTest {

  @Test
  public void test() throws Exception {
    new ItemComboBoxModel();
    final List<Item<Integer>> items = Arrays.asList(
            new Item<Integer>(null, ""),
            new Item<Integer>(1, "AOne"),
            new Item<Integer>(2, "BTwo"),
            new Item<Integer>(3, "CThree"),
            new Item<Integer>(4, "DFour"));
    final ItemComboBoxModel<Integer> model = new ItemComboBoxModel<Integer>(items) {
      @Override
      protected List<Item<Integer>> initializeContents() {
        return items;
      }
    };

    assertEquals("The item representing null should be at index 0", 0, model.getIndexOfItem(null));
    assertEquals("The item representing 1 should be at index 1", 1, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 2", 2, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 3", 3, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 4", 4, model.getIndexOfItem(4));

    model.setSelectedItem(1);
    assertTrue("The item representing 1 should be selected", model.getSelectedItem().equals(items.get(1)));
    assertEquals(1, (int) model.getSelectedValue().getItem());
    assertEquals("The item representing 1 should be selected", "AOne", model.getSelectedItem().toString());
    model.setSelectedItem(2);
    assertEquals(2, (int) model.getSelectedValue().getItem());
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(items.get(2)));
    model.setSelectedItem(4);
    assertEquals(4, (int) model.getSelectedValue().getItem());
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(items.get(4)));
    model.setSelectedItem(null);
    assertEquals(null, model.getSelectedValue().getItem());
    assertTrue("The item representing null should be selected", model.getSelectedItem().equals(items.get(0)));

    final ImageIcon icon = Images.loadImage("jminor_logo32.gif");
    final ItemComboBoxModel<String> iconModel = new ItemComboBoxModel<String>(new ItemComboBoxModel.IconItem<String>("test", icon));
    iconModel.setSelectedItem("test");
    final ItemComboBoxModel.IconItem item = (ItemComboBoxModel.IconItem) iconModel.getSelectedItem();
    assertEquals(icon.getIconHeight(), item.getIconHeight());
    assertEquals(icon.getIconWidth(), item.getIconWidth());
    assertEquals("", item.toString());

    model.clear();
    assertEquals(0, model.getSize());
    model.refresh();

    assertEquals("The item representing null should be at index 0", 0, model.getIndexOfItem(null));
    assertEquals("The item representing 1 should be at index 1", 1, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 2", 2, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 3", 3, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 4", 4, model.getIndexOfItem(4));
  }
}
