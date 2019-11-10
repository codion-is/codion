/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

/**
 * A column filter panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnConditionPanel<Property> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnConditionModel<Property> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   * @param includeToggleFilterEnabledButton if true an activation button is include
   * @param includeToggleAdvancedFilterButton if true an advanced toggle button is include
   */
  public PropertyFilterPanel(final ColumnConditionModel<Property> model, final boolean includeToggleFilterEnabledButton,
                             final boolean includeToggleAdvancedFilterButton) {
    super(model, includeToggleFilterEnabledButton, includeToggleAdvancedFilterButton, getConditionTypes(model));
  }

  private static ConditionType[] getConditionTypes(final ColumnConditionModel<Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new ConditionType[] {ConditionType.LIKE};
    }
    else {
      return ConditionType.values();
    }
  }
}