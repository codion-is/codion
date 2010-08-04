package org.jminor.common.ui;

import org.jminor.common.model.DefaultSearchModel;
import org.jminor.common.model.SearchModel;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Types;

public class AbstractSearchPanelTest {

  @Test
  public void test() {
    final String key = "key";
    final SearchModel<String> model = new DefaultSearchModel<String>(key, Types.VARCHAR, "%");
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

  private static class SearchPanelImpl extends AbstractSearchPanel<String> {

    private SearchPanelImpl(final SearchModel<String> objectSearchModel, final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
      super(objectSearchModel, includeActivateBtn, includeToggleAdvBtn);
    }

    @Override
    protected boolean isLowerBoundFieldRequired(final String searchKey) {
      return true;
    }
  }
}
