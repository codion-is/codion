/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

/**
 * A column filter panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnConditionPanel<Entity, Property<?>> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnConditionModel<Entity, Property<?>> model) {
    super(model, ToggleAdvancedButton.YES, getOperators(model));
  }

  private static Operator[] getOperators(final ColumnConditionModel<Entity, Property<?>> model) {
    if (model.getColumnIdentifier().getAttribute().isBoolean()) {
      return new Operator[] {Operator.EQUAL_TO};
    }

    return Operator.values();
  }
}