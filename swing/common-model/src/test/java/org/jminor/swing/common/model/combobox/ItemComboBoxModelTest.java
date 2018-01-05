/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Item;

import org.junit.Test;

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

    assertEquals("The item representing null should be at index 0", 0, model.indexOf(null));
    assertEquals("The item representing 1 should be at index 1", 1, model.indexOf(1));
    assertEquals("The item representing 2 should be at index 2", 2, model.indexOf(2));
    assertEquals("The item representing 3 should be at index 3", 3, model.indexOf(3));
    assertEquals("The item representing 4 should be at index 4", 4, model.indexOf(4));

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

    model.clear();
    assertEquals(0, model.getSize());
    model.refresh();

    assertEquals("The item representing null should be at index 0", 0, model.indexOf(null));
    assertEquals("The item representing 1 should be at index 1", 1, model.indexOf(1));
    assertEquals("The item representing 2 should be at index 2", 2, model.indexOf(2));
    assertEquals("The item representing 3 should be at index 3", 3, model.indexOf(3));
    assertEquals("The item representing 4 should be at index 4", 4, model.indexOf(4));

    //test unsorted final List<Item<Integer>> items = Arrays.asList(nullItem, cThree, bTwo, aOne, dFour);
    final ItemComboBoxModel<Integer> unsortedModel = new ItemComboBoxModel<Integer>(null, items) {
      @Override
      protected List<Item<Integer>> initializeContents() {
        return items;
      }
    };

    assertEquals("The item representing null should be at index 0", 0, unsortedModel.indexOf(null));
    assertEquals("The item representing 3 should be at index 1", 1, unsortedModel.indexOf(3));
    assertEquals("The item representing 2 should be at index 2", 2, unsortedModel.indexOf(2));
    assertEquals("The item representing 1 should be at index 3", 3, unsortedModel.indexOf(1));
    assertEquals("The item representing 4 should be at index 4", 4, unsortedModel.indexOf(4));
  }
}
