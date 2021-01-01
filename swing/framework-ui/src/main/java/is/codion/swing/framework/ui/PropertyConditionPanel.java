/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import static is.codion.swing.framework.ui.EntityInputComponents.createInputComponent;
import static java.util.Objects.requireNonNull;

/**
 * A column condition panel based on the Property class.
 * @param <C> the property type
 * @param <T> the column value type
 */
public final class PropertyConditionPanel<C extends Property<T>, T> extends ColumnConditionPanel<Entity, C, T> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param columnConditionModel the model to base this panel on
   */
  public PropertyConditionPanel(final ColumnConditionModel<Entity, C, T> columnConditionModel) {
    super(columnConditionModel, ToggleAdvancedButton.NO,
            new PropertyBoundFieldFactory<>(columnConditionModel), getOperators(columnConditionModel));
  }

  private static <C extends Property<T>, T> Operator[] getOperators(final ColumnConditionModel<Entity, C, T> model) {
    if (model.getColumnIdentifier().getAttribute().isBoolean()) {
      return new Operator[] {Operator.EQUAL};
    }

    return Operator.values();
  }

  private static final class PropertyBoundFieldFactory<C extends Property<T>, T> implements BoundFieldFactory {

    private final ColumnConditionModel<Entity, C, T> conditionModel;

    private PropertyBoundFieldFactory(final ColumnConditionModel<Entity, C, T> conditionModel) {
      this.conditionModel = requireNonNull(conditionModel);
    }

    @Override
    public JComponent createEqualField() {
      final JComponent component = createInputComponent(conditionModel.getColumnIdentifier(), conditionModel.getEqualValueSet().value());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }

    @Override
    public JComponent createUpperBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no upper bound field required for booleans
      }

      final JComponent component = createInputComponent(conditionModel.getColumnIdentifier(), conditionModel.getUpperBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }

    @Override
    public JComponent createLowerBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for booleans
      }

      final JComponent component = createInputComponent(conditionModel.getColumnIdentifier(), conditionModel.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
