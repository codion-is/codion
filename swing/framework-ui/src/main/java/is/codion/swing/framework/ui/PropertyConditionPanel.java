/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.common.value.Values;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.swing.common.ui.table.ColumnConditionPanel;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import java.util.Collections;

/**
 * A column condition panel based on the Property class.
 */
public final class PropertyConditionPanel<T> extends ColumnConditionPanel<Entity, ColumnProperty<T>, T> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   */
  public PropertyConditionPanel(final ColumnConditionModel<Entity, ColumnProperty<T>, T> model) {
    super(model, ToggleAdvancedButton.NO, new PropertyBoundFieldProvider<>(model), getOperators(model));
  }

  private static <T> Operator[] getOperators(final ColumnConditionModel<Entity, ColumnProperty<T>, T> model) {
    if (model.getColumnIdentifier().getAttribute().isBoolean()) {
      return new Operator[] {Operator.EQUAL};
    }

    return Operator.values();
  }

  private static final class PropertyBoundFieldProvider<T> implements BoundFieldProvider {

    private final ColumnConditionModel<Entity, ColumnProperty<T>, T> model;

    private PropertyBoundFieldProvider(final ColumnConditionModel<Entity, ColumnProperty<T>, T> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeEqualsValueField() {
      final ValueSet<T> valueSet = model.getEqualValueSet();
      final Value<T> value = Values.value();
      value.addDataListener(object -> valueSet.set(object == null ? Collections.emptySet() : Collections.singleton(object)));

      final JComponent component = EntityInputComponents.createInputComponent(model.getColumnIdentifier(), value);
      if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
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
