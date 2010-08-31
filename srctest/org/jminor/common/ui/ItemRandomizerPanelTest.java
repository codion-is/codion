/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.ItemRandomizer;
import org.jminor.common.model.ItemRandomizerModel;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ItemRandomizerPanelTest {

  @Test
  public void test() {
    final ItemRandomizer<String> model = new ItemRandomizerModel<String>(5, "one", "two", "three");
    final ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<String>(model);
    assertEquals(model, panel.getModel());
  }
}
