/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ColumnConditionPanel.ToggleAdvancedButton;
import is.codion.swing.common.ui.table.ConditionPanelFactory;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;
import is.codion.swing.framework.ui.component.EntityInputComponents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;

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
    final ColumnConditionModel<Attribute<T>, T> conditionModel = (ColumnConditionModel<Attribute<T>, T>)
            tableConditionModel.getConditionModels().get(column.getIdentifier());

    return conditionModel == null ? null : createConditionPanel(conditionModel);
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
    final ColumnConditionPanel.BoundFieldFactory boundFieldFactory;
    if (conditionModel instanceof ForeignKeyConditionModel) {
      boundFieldFactory = new ForeignKeyBoundFieldFactory((ForeignKeyConditionModel) conditionModel);
    }
    else {
      boundFieldFactory = new AttributeBoundFieldFactory<>(conditionModel, entityInputComponents, conditionModel.getColumnIdentifier());
    }
    try {
      return new ColumnConditionPanel<>(conditionModel, ToggleAdvancedButton.NO, boundFieldFactory);
    }
    catch (final IllegalArgumentException e) {
      LOG.error("Unable to create AttributeConditionPanel for attribute: " + conditionModel.getColumnIdentifier(), e);
      return null;
    }
  }

  private static final class ForeignKeyBoundFieldFactory implements ColumnConditionPanel.BoundFieldFactory {

    private final ColumnConditionModel<ForeignKey, Entity> model;

    private ForeignKeyBoundFieldFactory(final ColumnConditionModel<ForeignKey, Entity> model) {
      this.model = model;
    }

    @Override
    public JComponent createEqualField() {
      return Sizes.setPreferredHeight(createForeignKeyField(), TextFields.getPreferredTextFieldHeight());
    }

    @Override
    public JComponent createUpperBoundField() {
      return null;
    }

    @Override
    public JComponent createLowerBoundField() {
      return null;
    }

    private JComponent createForeignKeyField() {
      if (model instanceof SwingForeignKeyConditionModel) {
        return Completion.maximumMatch(new EntityComboBox(((SwingForeignKeyConditionModel) model).getEntityComboBoxModel()).refreshOnSetVisible());
      }

      return TextFields.selectAllOnFocusGained(new EntitySearchField(((ForeignKeyConditionModel) model).getEntitySearchModel()));
    }
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
      final ComponentValue<T, JComponent> componentValue = inputComponents.createInputComponent(attribute);
      componentValue.link(conditionModel.getEqualValueSet().value());

      return configureComponent(componentValue.getComponent());
    }

    @Override
    public JComponent createUpperBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no upper bound field required for booleans
      }

      final ComponentValue<T, JComponent> componentValue = inputComponents.createInputComponent(attribute);
      componentValue.link(conditionModel.getUpperBoundValue());

      return configureComponent(componentValue.getComponent());
    }

    @Override
    public JComponent createLowerBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return null;//no lower bound field required for booleans
      }

      final ComponentValue<T, JComponent> componentValue = inputComponents.createInputComponent(attribute);
      componentValue.link(conditionModel.getLowerBoundValue());

      return configureComponent(componentValue.getComponent());
    }

    private JComponent configureComponent(final JComponent component) {
      if (component instanceof JTextField) {
        ((JTextField) component).setColumns(0);
        ((JTextField) component).setHorizontalAlignment(SwingConstants.CENTER);
      }
      else if (component instanceof JCheckBox) {
        ((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
      }

      return component;
    }
  }
}
