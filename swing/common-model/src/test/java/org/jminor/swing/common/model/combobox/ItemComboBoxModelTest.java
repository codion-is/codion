/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.combobox;

import org.jminor.common.Item;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    assertEquals(0, model.indexOf(null));
    assertEquals(1, model.indexOf(1));
    assertEquals(2, model.indexOf(2));
    assertEquals(3, model.indexOf(3));
    assertEquals(4, model.indexOf(4));

    model.setSelectedItem(1);
    assertEquals(model.getSelectedItem(), aOne);
    assertEquals(1, (int) model.getSelectedValue().getItem());
    assertEquals("AOne", model.getSelectedItem().toString());
    model.setSelectedItem(2);
    assertEquals(2, (int) model.getSelectedValue().getItem());
    assertEquals(model.getSelectedItem(), bTwo);
    model.setSelectedItem(4);
    assertEquals(4, (int) model.getSelectedValue().getItem());
    assertEquals(model.getSelectedItem(), dFour);
    model.setSelectedItem(null);
    assertNull(model.getSelectedValue().getItem());
    assertEquals(model.getSelectedItem(), nullItem);

    model.clear();
    assertEquals(0, model.getSize());
    model.refresh();

    assertEquals(0, model.indexOf(null));
    assertEquals(1, model.indexOf(1));
    assertEquals(2, model.indexOf(2));
    assertEquals(3, model.indexOf(3));
    assertEquals(4, model.indexOf(4));

    //test unsorted final List<Item<Integer>> items = Arrays.asList(nullItem, cThree, bTwo, aOne, dFour);
    final ItemComboBoxModel<Integer> unsortedModel = new ItemComboBoxModel<Integer>(null, items) {
      @Override
      protected List<Item<Integer>> initializeContents() {
        return items;
      }
    };

    assertEquals(0, unsortedModel.indexOf(null));
    assertEquals(1, unsortedModel.indexOf(3));
    assertEquals(2, unsortedModel.indexOf(2));
    assertEquals(3, unsortedModel.indexOf(1));
    assertEquals(4, unsortedModel.indexOf(4));
  }
}
