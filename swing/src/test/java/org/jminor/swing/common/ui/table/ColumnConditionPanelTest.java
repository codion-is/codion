/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.db.condition.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.*;

public class ColumnConditionPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnConditionModel<String> model = new DefaultColumnConditionModel<>(key, Types.VARCHAR, "%");
    final ColumnConditionPanel<String> panel = new ColumnConditionPanel<>(model, true, true);
    assertEquals(model, panel.getConditionModel());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertNull(panel.getLastDialogPosition());
    assertFalse(panel.isDialogEnabled());
    assertFalse(panel.isDialogVisible());
    assertFalse(panel.isAdvancedConditionEnabled());
    panel.setAdvancedConditionEnabled(true);
    assertTrue(panel.isAdvancedConditionEnabled());
    panel.setAdvancedConditionEnabled(false);
    assertFalse(panel.isAdvancedConditionEnabled());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullConditionModel() {
    new ColumnConditionPanel<String>(null, true, true, (ConditionType) null);
  }
}
