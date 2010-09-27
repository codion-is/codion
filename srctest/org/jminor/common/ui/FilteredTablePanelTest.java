/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.AbstractFilteredTableModelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class FilteredTablePanelTest {

  @Test
  public void test() {
    final FilteredTablePanel<String, Integer> panel =
            new FilteredTablePanel<String, Integer>(AbstractFilteredTableModelTest.createTestModel());
    assertEquals(1, panel.getColumnFilterPanels().size());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }
}
