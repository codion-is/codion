package org.jminor.common.ui;

import org.jminor.common.model.AbstractFilteredTableModelTest;
import org.jminor.common.model.ColumnSearchModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class AbstractFilteredTablePanelTest {

  @Test
  public void test() {
    final AbstractFilteredTablePanel<String, Integer> panel =
            new AbstractFilteredTablePanel<String, Integer>(AbstractFilteredTableModelTest.createTestModel()) {
      @Override
      protected ColumnSearchPanel<Integer> initializeFilterPanel(final ColumnSearchModel<Integer> model) {
        return new ColumnSearchPanel<Integer>(model, true, true);
      }
    };
    assertEquals(1, panel.getColumnFilterPanels().size());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }
}
