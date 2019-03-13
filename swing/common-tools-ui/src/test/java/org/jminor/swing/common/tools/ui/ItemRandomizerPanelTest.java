/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.ui;

import org.jminor.swing.common.tools.ItemRandomizer;
import org.jminor.swing.common.tools.ItemRandomizerModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemRandomizerPanelTest {

  @Test
  public void test() {
    final ItemRandomizer<String> model = new ItemRandomizerModel<>(5, "one", "two", "three");
    final ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<>(model);
    assertEquals(model, panel.getModel());
  }

  @Test
  public void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new ItemRandomizerPanel<String>(null));
  }
}
