/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.Operator;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.Property;
import dev.codion.swing.common.ui.table.ColumnConditionPanel;

/**
 * A column filter panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnConditionPanel<Entity, Property> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnConditionModel<Entity, Property> model) {
    super(model, ToggleAdvancedButton.YES, getOperators(model));
  }

  private static Operator[] getOperators(final ColumnConditionModel<Entity, Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.LIKE};
    }

    return Operator.values();
  }
}