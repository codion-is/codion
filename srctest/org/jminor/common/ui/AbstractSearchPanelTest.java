package org.jminor.common.ui;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.DefaultColumnSearchModel;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Types;

public class AbstractSearchPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final ColumnSearchModel<String> model = new DefaultColumnSearchModel<String>(key, Types.VARCHAR, "%");
    final SearchPanelImpl panel = new SearchPanelImpl(model, true, true);
    assertEquals(model, panel.getModel());
    assertNotNull(panel.getUpperBoundField());
    assertNotNull(panel.getLowerBoundField());
    assertNull(panel.getLastPosition());
    assertFalse(panel.isDialogActive());
    assertFalse(panel.isDialogShowing());
    assertFalse(panel.isAdvancedSearchOn());
    panel.setAdvancedSearchOn(true);
    assertTrue(panel.getAdvancedSearchState().isActive());
    panel.setAdvancedSearchOn(false);
    assertFalse(panel.isAdvancedSearchOn());
  }

  private static class SearchPanelImpl extends ColumnSearchPanel<String> {

    private SearchPanelImpl(final ColumnSearchModel<String> objectSearchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
      super(objectSearchModel, includeActivateBtn, includeToggleAdvBtn);
    }
  }
}
