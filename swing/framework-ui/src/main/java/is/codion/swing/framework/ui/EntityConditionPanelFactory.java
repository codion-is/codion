/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ColumnConditionPanel.ToggleAdvancedButton;
import is.codion.swing.common.ui.table.ConditionPanelFactory;
import is.codion.swing.framework.ui.builder.EntityInputComponents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ConditionPanelFactory} implementation.
 * Override {@link #createConditionPanel(ColumnConditionModel)} to provide custom condition panels.
 */
public class EntityConditionPanelFactory implements ConditionPanelFactory {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConditionPanelFactory.class);

  private final EntityTableConditionModel tableConditionModel;
  private final EntityInputComponents entityInputComponents;

  /**
   * Instantiates a new {@link EntityConditionPanelFactory}
   * @param tableConditionModel the table condition model
   */
  public EntityConditionPanelFactory(final EntityTableConditionModel tableConditionModel) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
    this.entityInputComponents = new EntityInputComponents(tableConditionModel.getEntityDefinition());
  }

  @Override
  public final <T> ColumnConditionPanel<?, T> createConditionPanel(final TableColumn column) {
    final Attribute<T> attribute = (Attribute<T>) column.getIdentifier();
    if (tableConditionModel.containsConditionModel(attribute)) {
      return createConditionPanel(tableConditionModel.getConditionModel(attribute));
    }

    return null;
  }

  /**
   * Initializes a ColumnConditionPanel for the given model
   * @param <C> the column identifier type
   * @param <T> the column value type
   * @param conditionModel the {@link ColumnConditionModel} for which to create a condition panel, not null
   * @return a ColumnConditionPanel based on the given model
   * @see #createDefaultConditionPanel(ColumnConditionModel)
   */
  protected <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createConditionPanel(final ColumnConditionModel<C, T> conditionModel) {
    return createDefaultConditionPanel(requireNonNull(conditionModel));
  }

  /**
   * Initializes a ColumnConditionPanel for the given model
   * @param <C> the column identifier type
   * @param <T> the column value type
   * @param conditionModel the {@link ColumnConditionModel} for which to create a condition panel
   * @return a ColumnConditionPanel based on the given model
   */
  protected final <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createDefaultConditionPanel(final ColumnConditionModel<C, T> conditionModel) {
    if (conditionModel instanceof ForeignKeyConditionModel) {
      return (ColumnConditionPanel<C, T>) new ForeignKeyConditionPanel((ForeignKeyConditionModel) conditionModel);
    }

    final ColumnConditionPanel.BoundFieldFactory boundFieldFactory =
            new AttributeBoundFieldFactory<>(conditionModel, entityInputComponents, conditionModel.getColumnIdentifier());
    try {
      return new ColumnConditionPanel<>(conditionModel, ToggleAdvancedButton.NO, boundFieldFactory, getOperators(conditionModel));
    }
    catch (final IllegalArgumentException e) {
      LOG.error("Unable to create AttributeConditionPanel for attribute: " + conditionModel.getColumnIdentifier(), e);
      return null;
    }
  }

  private static <C extends Attribute<?>> List<Operator> getOperators(final ColumnConditionModel<C, ?> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }

  private static final class AttributeBoundFieldFactory<C extends Attribute<T>, T> implements ColumnConditionPanel.BoundFieldFactory {

    private final ColumnConditionModel<C, T> conditionModel;
    private final EntityInputComponents inputComponents;
    private final Attribute<T> attribute;

    private AttributeBoundFieldFactory(final ColumnConditionModel<C, T> conditionModel,
                                       final EntityInputComponents inputComponents,
                                       final Attribute<T> attribute) {
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
