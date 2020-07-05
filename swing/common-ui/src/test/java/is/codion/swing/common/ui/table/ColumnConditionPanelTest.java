/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.swing.common.ui.table.ColumnConditionPanel.ToggleAdvancedButton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnConditionModel<String, String, String> model = new DefaultColumnConditionModel<>(key, String.class, "%");
    final ColumnConditionPanel<String, String, String> panel = new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES);
    assertEquals(model, panel.getModel());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertNull(panel.getLastDialogPosition());
    assertFalse(panel.isDialogEnabled());
    assertFalse(panel.isDialogVisible());
    assertFalse(panel.isAdvanced());
    panel.setAdvanced(true);
    assertTrue(panel.isAdvanced());
    panel.setAdvanced(false);
    assertFalse(panel.isAdvanced());
  }

  @Test
  public void constructorNullConditionModel() {
    assertThrows(NullPointerException.class, () -> new ColumnConditionPanel<String, String, String>(null, ToggleAdvancedButton.YES, (Operator) null));
  }
}
