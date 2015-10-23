/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.*;

public class ColumnCriteriaPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnCriteriaModel<String> model = new DefaultColumnCriteriaModel<>(key, Types.VARCHAR, "%");
    final ColumnCriteriaPanel<String> panel = new ColumnCriteriaPanel<>(model, true, true);
    assertEquals(model, panel.getCriteriaModel());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertNull(panel.getLastDialogPosition());
    assertFalse(panel.isDialogEnabled());
    assertFalse(panel.isDialogVisible());
    assertFalse(panel.isAdvancedCriteriaEnabled());
    panel.setAdvancedCriteriaEnabled(true);
    assertTrue(panel.isAdvancedCriteriaEnabled());
    panel.setAdvancedCriteriaEnabled(false);
    assertFalse(panel.isAdvancedCriteriaEnabled());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullCriteriaModel() {
    new ColumnCriteriaPanel<String>(null, true, true, (SearchType) null);
  }
}
