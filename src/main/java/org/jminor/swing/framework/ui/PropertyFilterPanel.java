/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Property;
import org.jminor.swing.common.model.table.ColumnCriteriaModel;
import org.jminor.swing.common.ui.table.ColumnCriteriaPanel;

/**
 * A column filter panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnCriteriaPanel<Property> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnCriteriaModel<Property> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   * @param includeToggleFilterEnabledButton if true an activation button is include
   * @param includeToggleAdvancedFilterButton if true an advanced toggle button is include
   */
  public PropertyFilterPanel(final ColumnCriteriaModel<Property> model, final boolean includeToggleFilterEnabledButton,
                             final boolean includeToggleAdvancedFilterButton) {
    super(model, includeToggleFilterEnabledButton, includeToggleAdvancedFilterButton, getSearchTypes(model));
  }

  private static SearchType[] getSearchTypes(final ColumnCriteriaModel<Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new SearchType[] {SearchType.LIKE};
    }
    else {
      return SearchType.values();
    }
  }
}