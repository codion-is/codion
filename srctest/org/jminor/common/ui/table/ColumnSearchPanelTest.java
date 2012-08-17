/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.model.table.DefaultColumnSearchModel;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.*;

public class ColumnSearchPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnSearchModel<String> model = new DefaultColumnSearchModel<String>(key, Types.VARCHAR, "%");
    final ColumnSearchPanel<String> panel = new ColumnSearchPanel<String>(model, true, true);
    assertEquals(model, panel.getSearchModel());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertNull(panel.getLastDialogPosition());
    assertFalse(panel.isDialogEnabled());
    assertFalse(panel.isDialogVisible());
    assertFalse(panel.isAdvancedSearchOn());
    panel.setAdvancedSearchOn(true);
    assertTrue(panel.isAdvancedSearchOn());
    panel.setAdvancedSearchOn(false);
    assertFalse(panel.isAdvancedSearchOn());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullSearchModel() {
    new ColumnSearchPanel<String>(null, true, true, (SearchType) null);
  }
}
