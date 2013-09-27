/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.tools;

import org.jminor.common.model.tools.ItemRandomizer;
import org.jminor.common.model.tools.ItemRandomizerModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemRandomizerPanelTest {

  @Test
  public void test() {
    final ItemRandomizer<String> model = new ItemRandomizerModel<>(5, "one", "two", "three");
    final ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<>(model);
    assertEquals(model, panel.getModel());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullModel() {
    new ItemRandomizerPanel<String>(null);
  }
}
