/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.jminor.common.model.table.AbstractFilteredTableModelTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class FilteredTablePanelTest {

  @Test
  public void test() {
    final FilteredTablePanel<String, Integer> panel =
            new FilteredTablePanel<>(AbstractFilteredTableModelTest.createTestModel());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTableModel() {
    new FilteredTablePanel<String, Integer>(null, null);
  }
}
