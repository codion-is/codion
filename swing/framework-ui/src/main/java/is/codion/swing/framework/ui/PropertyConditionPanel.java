/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.Operator;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A column condition panel based on the Property class.
 */
public final class PropertyConditionPanel extends ColumnConditionPanel<Entity, ColumnProperty> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   */
  public PropertyConditionPanel(final ColumnConditionModel<Entity, ColumnProperty> model) {
    super(model, ToggleAdvancedButton.NO, new PropertyBoundFieldProvider(model), getOperators(model));
  }

  private static Operator[] getOperators(final ColumnConditionModel<Entity, ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.LIKE};
    }

    return Operator.values();
  }

  private static final class PropertyBoundFieldProvider implements BoundFieldProvider {

    private final ColumnConditionModel<Entity, ColumnProperty> model;

    private PropertyBoundFieldProvider(final ColumnConditionModel<Entity, ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeUpperBoundField() {
      final JComponent component = EntityInputComponents.createInputComponent(model.getColumnIdentifier(), model.getUpperBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }

    @Override
    public JComponent initializeLowerBoundField() {
      if (model.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for booleans
      }

      final JComponent component = EntityInputComponents.createInputComponent(model.getColumnIdentifier(), model.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
