/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A column condition panel based on the Property class.
 */
public final class PropertyConditionPanel extends ColumnConditionPanel<ColumnProperty> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   */
  public PropertyConditionPanel(final ColumnConditionModel<ColumnProperty> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public PropertyConditionPanel(final ColumnConditionModel<ColumnProperty> model,
                                final boolean includeToggleEnabledButton, final boolean includeToggleAdvancedConditionButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedConditionButton,
            new PropertyInputFieldProvider(model), getConditionTypes(model));
  }

  private static ConditionType[] getConditionTypes(final ColumnConditionModel<ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new ConditionType[] {ConditionType.LIKE};
    }
    else {
      return ConditionType.values();
    }
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnConditionModel<ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getTypeClass().equals(Boolean.class) && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }

      final JComponent component = EntityUiUtil.createInputComponent(model.getColumnIdentifier(),
              isUpperBound ? model.getUpperBoundValue() : model.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
