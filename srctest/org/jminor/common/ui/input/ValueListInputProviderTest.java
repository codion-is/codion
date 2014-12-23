/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValueListInputProviderTest {

  @Test
  public void test() {
    final List<Item<String>> items = Arrays.asList(new Item<String>(null), new Item<>("one"), new Item<>("two"), new Item<>("three"), new Item<>("four"));
    ValueListInputProvider<String> inputProvider = new ValueListInputProvider<>("two", items);
    ItemComboBoxModel<String> boxModel = (ItemComboBoxModel<String>) inputProvider.getInputComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertEquals("two", inputProvider.getValue());

    inputProvider = new ValueListInputProvider<>(null, items);
    boxModel = (ItemComboBoxModel<String>) inputProvider.getInputComponent().getModel();
    assertEquals(5, boxModel.getSize());
    assertNull(inputProvider.getValue());
  }
}
