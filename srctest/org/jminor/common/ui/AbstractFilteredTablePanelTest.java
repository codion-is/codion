package org.jminor.common.ui;

import org.jminor.common.model.AbstractFilteredTableModelTest;
import org.jminor.common.model.SearchModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class AbstractFilteredTablePanelTest {

  @Test
  public void test() {
    final AbstractFilteredTablePanel<String, Integer> panel =
            new AbstractFilteredTablePanel<String, Integer>(AbstractFilteredTableModelTest.createTestModel()) {
      @Override
      protected AbstractSearchPanel<Integer> initializeFilterPanel(final SearchModel<Integer> model) {
        return new AbstractSearchPanel<Integer>(model, true, true) {          
          @Override
          protected boolean isLowerBoundFieldRequired(final Integer searchKey) {
            return true;
          }
        };
      }
    };
    assertEquals(1, panel.getColumnFilterPanels().size());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }
}
