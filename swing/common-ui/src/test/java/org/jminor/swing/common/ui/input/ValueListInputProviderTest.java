/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.Item;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ValueListInputProviderTest {

  @Test
  public void test() {
    final List<Item<String>> items = asList(new Item<>(null), new Item<>("one"), new Item<>("two"), new Item<>("three"), new Item<>("four"));
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
