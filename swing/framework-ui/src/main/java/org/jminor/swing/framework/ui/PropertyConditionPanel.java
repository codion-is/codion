/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

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
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public PropertyConditionPanel(final ColumnConditionModel<Entity, ColumnProperty> model,
                                final boolean includeToggleEnabledButton, final boolean includeToggleAdvancedConditionButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedConditionButton,
            new PropertyInputFieldProvider(model), getConditionTypes(model));
  }

  private static ConditionType[] getConditionTypes(final ColumnConditionModel<Entity, ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new ConditionType[] {ConditionType.LIKE};
    }

    return ConditionType.values();
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<Entity, ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnConditionModel<Entity, ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getTypeClass().equals(Boolean.class) && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }

      final JComponent component = EntityInputComponents.createInputComponent(model.getColumnIdentifier(),
              isUpperBound ? model.getUpperBoundValue() : model.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
