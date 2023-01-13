/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.ui.randomizer;

import is.codion.swing.common.tools.randomizer.ItemRandomizer;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemRandomizerPanelTest {

  @Test
  void test() {
    ItemRandomizer<String> model = ItemRandomizer.itemRandomizer(Arrays.asList(
            ItemRandomizer.RandomItem.randomItem("one", 5),
            ItemRandomizer.RandomItem.randomItem("two", 5),
            ItemRandomizer.RandomItem.randomItem("three", 5)
    ));
    ItemRandomizerPanel<String> panel = ItemRandomizerPanel.itemRandomizerPanel(model);
    assertEquals(model, panel.itemRandomizer());
  }

  @Test
  void constructorNullModel() {
    assertThrows(NullPointerException.class, () -> ItemRandomizerPanel.itemRandomizerPanel(null));
  }
}
