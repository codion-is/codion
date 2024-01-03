/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

  @Test
  void test() {
    final String key = "key";
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder(key, String.class).build();
    ColumnConditionPanel<String, String> panel = columnConditionPanel(model).orElse(null);
    assertEquals(model, panel.model());
    assertNotNull(panel.equalField());
    assertNotNull(panel.upperBoundField());
    assertNotNull(panel.lowerBoundField());
    assertThrows(NullPointerException.class, () -> ColumnConditionPanel.<String, String>columnConditionPanel(null, null));
  }

  @Test
  void lockedModel() {
    ColumnConditionModel<String, String> model = ColumnConditionModel.builder("key", String.class).build();
    model.locked().set(true);
    columnConditionPanel(model);
  }
}
