/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnConditionPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnConditionModel<String, String> model = new DefaultColumnConditionModel<>(key, String.class, "%");
    final ColumnConditionPanel<String, String> panel = new ColumnConditionPanel<>(model, true);
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
    assertThrows(NullPointerException.class, () -> new ColumnConditionPanel<String, String>(null, true, (ConditionType) null));
  }
}
