/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

import static is.codion.swing.framework.ui.EntityInputComponents.createInputComponent;
import static java.util.Objects.requireNonNull;

/**
 * A column condition panel based on an Attribute.
 * @param <C> the attribute type
 * @param <T> the column value type
 */
public final class AttributeConditionPanel<C extends Attribute<T>, T> extends ColumnConditionPanel<Entity, C, T> {

  /**
   * Instantiates a new AttributeConditionPanel.
   * @param columnConditionModel the model to base this panel on
   * @param property the underlying property
   */
  public AttributeConditionPanel(final ColumnConditionModel<Entity, C, T> columnConditionModel, final Property<T> property) {
    super(columnConditionModel, ToggleAdvancedButton.NO,
            new PropertyBoundFieldFactory<>(columnConditionModel, property), getOperators(columnConditionModel));
  }

  private static <C extends Attribute<T>, T> Operator[] getOperators(final ColumnConditionModel<Entity, C, T> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.EQUAL};
    }

    return Operator.values();
  }

  private static final class PropertyBoundFieldFactory<C extends Attribute<T>, T> implements BoundFieldFactory {

    private final ColumnConditionModel<Entity, C, T> conditionModel;
    private final Property<T> property;

    private PropertyBoundFieldFactory(final ColumnConditionModel<Entity, C, T> conditionModel, final Property<T> property) {
      this.conditionModel = requireNonNull(conditionModel);
      this.property = requireNonNull(property);
    }

    @Override
    public JComponent createEqualField() {
      final JComponent component = createInputComponent(property, conditionModel.getEqualValueSet().value());
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

      final JComponent component = createInputComponent(property, conditionModel.getUpperBoundValue());
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

      final JComponent component = createInputComponent(property, conditionModel.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
