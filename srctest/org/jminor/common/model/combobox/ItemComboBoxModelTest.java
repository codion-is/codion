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
    final Item<Integer> nullItem = new Item<>(null, "");
    final Item<Integer> aOne = new Item<>(1, "AOne");
    final Item<Integer> bTwo = new Item<>(2, "BTwo");
    final Item<Integer> cThree = new Item<>(3, "CThree");
    final Item<Integer> dFour = new Item<>(4, "DFour");

    final List<Item<Integer>> items = Arrays.asList(nullItem, cThree, bTwo, aOne, dFour);
    final ItemComboBoxModel<Integer> model = new ItemComboBoxModel<Integer>(items) {
      @Override
      protected List<Item<Integer>> initializeContents() {
        return items;//so we can clear the model later on without removing all items
      }
    };

    assertEquals("The item representing null should be at index 0", 0, model.getIndexOfItem(null));
    assertEquals("The item representing 1 should be at index 1", 1, model.getIndexOfItem(1));
    assertEquals("The item representing 2 should be at index 2", 2, model.getIndexOfItem(2));
    assertEquals("The item representing 3 should be at index 3", 3, model.getIndexOfItem(3));
    assertEquals("The item representing 4 should be at index 4", 4, model.getIndexOfItem(4));

    model.setSelectedItem(1);
    assertTrue("The item representing 1 should be selected", model.getSelectedItem().equals(aOne));
    assertEquals(1, (int) model.getSelectedValue().getItem());
    assertEquals("The item representing 1 should be selected", "AOne", model.getSelectedItem().toString());
    model.setSelectedItem(2);
    assertEquals(2, (int) model.getSelectedValue().getItem());
    assertTrue("The item representing 2 should be selected", model.getSelectedItem().equals(bTwo));
    model.setSelectedItem(4);
    assertEquals(4, (int) model.getSelectedValue().getItem());
    assertTrue("The item representing 4 should be selected", model.getSelectedItem().equals(dFour));
    model.setSelectedItem(null);
    assertEquals(null, model.getSelectedValue().getItem());
    assertTrue("The item representing null should be selected", model.getSelectedItem().equals(nullItem));

    final ImageIcon icon = Images.loadImage("jminor_logo32.gif");
    final ItemComboBoxModel<String> iconModel = new ItemComboBoxModel<>(Arrays.asList(new ItemComboBoxModel.IconItem<>("test", icon)));
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

    //test unsorted final List<Item<Integer>> items = Arrays.asList(nullItem, cThree, bTwo, aOne, dFour);
    final ItemComboBoxModel<Integer> unsortedModel = new ItemComboBoxModel<Integer>(null, items) {
      @Override
      protected List<Item<Integer>> initializeContents() {
        return items;
      }
    };

    assertEquals("The item representing null should be at index 0", 0, unsortedModel.getIndexOfItem(null));
    assertEquals("The item representing 3 should be at index 1", 1, unsortedModel.getIndexOfItem(3));
    assertEquals("The item representing 2 should be at index 2", 2, unsortedModel.getIndexOfItem(2));
    assertEquals("The item representing 1 should be at index 3", 3, unsortedModel.getIndexOfItem(1));
    assertEquals("The item representing 4 should be at index 4", 4, unsortedModel.getIndexOfItem(4));
  }
}
