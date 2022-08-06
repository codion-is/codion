/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.randomizer;

import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.tools.randomizer.ItemRandomizerModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemRandomizerPanelTest {

  @Test
  void test() {
    ItemRandomizer<String> model = new ItemRandomizerModel<>();
    model.addItem("one", 5);
    model.addItem("two", 5);
    model.addItem("three", 5);
    ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<>(model);
    assertEquals(model, panel.model());
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> new ItemRandomizerPanel<String>(null));
  }
}
