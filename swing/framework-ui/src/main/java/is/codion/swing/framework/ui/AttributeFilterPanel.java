/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

/**
 * A column filter panel based on an Attribute.
 * @param <C> the attribute type
 * @param <T> the column value type
 */
public final class AttributeFilterPanel<C extends Attribute<T>, T> extends ColumnConditionPanel<Entity, C, T> {

  /**
   * Instantiates a new AttributeFilterPanel.
   * @param model the model to base this panel on
   */
  public AttributeFilterPanel(final ColumnConditionModel<Entity, C, T> model) {
    super(model, ToggleAdvancedButton.YES, getOperators(model));
  }

  private static <C extends Attribute<T>, T> Operator[] getOperators(final ColumnConditionModel<Entity, C, T> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.EQUAL};
    }

    return Operator.values();
  }
}