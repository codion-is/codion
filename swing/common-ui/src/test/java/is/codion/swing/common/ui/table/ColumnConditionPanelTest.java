/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.swing.common.ui.table.ColumnConditionPanel.ToggleAdvancedButton;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

  @Test
  void test() {
    final String key = "key";
    final ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>(key, String.class, "%");
    final ColumnConditionPanel<String, String> panel = new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES);
    assertEquals(model, panel.getModel());
    assertNotNull(panel.getEqualField());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertFalse(panel.isDialogVisible());
    assertFalse(panel.isAdvanced());
    panel.setAdvanced(true);
    assertTrue(panel.isAdvanced());
    panel.setAdvanced(false);
    assertFalse(panel.isAdvanced());
    assertThrows(NullPointerException.class, () -> new ColumnConditionPanel<String, String>(null, ToggleAdvancedButton.YES, null));
    assertThrows(IllegalArgumentException.class, () -> new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES, Collections.emptyList()));
  }

  @Test
  void lockedModel() {
    final ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>("key", String.class, "%");
    model.setLocked(true);
    new ColumnConditionPanel<>(model, ToggleAdvancedButton.YES);
  }
}
