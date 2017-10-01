/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.tools;

import org.jminor.common.tools.ItemRandomizer;
import org.jminor.common.tools.ItemRandomizerModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemRandomizerPanelTest {

  @Test
  public void test() {
    final ItemRandomizer<String> model = new ItemRandomizerModel<>(5, "one", "two", "three");
    final ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<>(model);
    assertEquals(model, panel.getModel());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullModel() {
    new ItemRandomizerPanel<String>(null);
  }
}
