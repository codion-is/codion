/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.Operator;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

/**
 * A column filter panel based on properties.
 */
public final class PropertyFilterPanel extends ColumnConditionPanel<Entity, Property> {

  /**
   * Instantiates a new PropertyFilterPanel.
   * @param model the model to base this panel on
   */
  public PropertyFilterPanel(final ColumnConditionModel<Entity, Property> model) {
    super(model, true, getOperators(model));
  }

  private static Operator[] getOperators(final ColumnConditionModel<Entity, Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.LIKE};
    }

    return Operator.values();
  }
}