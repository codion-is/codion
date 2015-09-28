/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.swing.model.table.ColumnSearchModel;
import org.jminor.common.swing.ui.table.ColumnSearchPanel;
import org.jminor.framework.domain.Property;

/**
 * A column search panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnSearchPanel<Property> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnSearchModel<Property> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   * @param includeToggleFilterEnabledButton if true an activation button is include
   * @param includeToggleAdvancedFilterButton if true an advanced toggle button is include
   */
  public PropertyFilterPanel(final ColumnSearchModel<Property> model, final boolean includeToggleFilterEnabledButton,
                             final boolean includeToggleAdvancedFilterButton) {
    super(model, includeToggleFilterEnabledButton, includeToggleAdvancedFilterButton, getSearchTypes(model));
  }

  private static SearchType[] getSearchTypes(final ColumnSearchModel<Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new SearchType[] {SearchType.LIKE};
    }
    else {
      return SearchType.values();
    }
  }
}