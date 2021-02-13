/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

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
   * @param attribute the underlying attribute
   */
  public AttributeConditionPanel(final ColumnConditionModel<Entity, C, T> columnConditionModel,
          final EntityDefinition entityDefinition, final Attribute<T> attribute) {
    super(columnConditionModel, ToggleAdvancedButton.NO,
            new PropertyBoundFieldFactory<>(columnConditionModel, new EntityInputComponents(entityDefinition), attribute),
            getOperators(columnConditionModel));
  }

  private static <C extends Attribute<T>, T> Operator[] getOperators(final ColumnConditionModel<Entity, C, T> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Operator[] {Operator.EQUAL};
    }

    return Operator.values();
  }

  private static final class PropertyBoundFieldFactory<C extends Attribute<T>, T> implements BoundFieldFactory {

    private final ColumnConditionModel<Entity, C, T> conditionModel;
    private final EntityInputComponents inputComponents;
    private final Attribute<T> attribute;

    private PropertyBoundFieldFactory(final ColumnConditionModel<Entity, C, T> conditionModel,
            final EntityInputComponents inputComponents, final Attribute<T> attribute) {
      this.conditionModel = requireNonNull(conditionModel);
      this.inputComponents = inputComponents;
      this.attribute = requireNonNull(attribute);
    }

    @Override
    public JComponent createEqualField() {
      final JComponent component = inputComponents.createInputComponent(attribute, conditionModel.getEqualValueSet().value());
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

      final JComponent component = inputComponents.createInputComponent(attribute, conditionModel.getUpperBoundValue());
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

      final JComponent component = inputComponents.createInputComponent(attribute, conditionModel.getLowerBoundValue());
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
