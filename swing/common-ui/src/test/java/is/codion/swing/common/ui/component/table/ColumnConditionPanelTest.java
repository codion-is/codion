/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ToggleAdvancedButton;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

  private static final List<Operator> ALL_OPERATORS = asList(Operator.values());

  @Test
  void test() {
    final String key = "key";
    ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>(key, String.class, ALL_OPERATORS, '%');
    ColumnConditionPanel<String, String> panel = new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES);
    assertEquals(model, panel.getModel());
    assertNotNull(panel.getEqualField());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertFalse(panel.isDialogVisible());
    assertThrows(NullPointerException.class, () -> new ColumnConditionPanel<String, String>(null, null));
  }

  @Test
  void lockedModel() {
    ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("key", String.class, ALL_OPERATORS, '%');
    model.setLocked(true);
    new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES);
  }
}
