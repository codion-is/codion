/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.randomizer;

import is.codion.swing.common.tools.randomizer.ItemRandomizer;
import is.codion.swing.common.tools.randomizer.ItemRandomizerModel;

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
