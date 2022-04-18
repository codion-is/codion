/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ToggleAdvancedButton;
import is.codion.swing.common.ui.component.table.ConditionPanelFactory;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingForeignKeyConditionModel;
import is.codion.swing.framework.ui.component.EntityComponents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ConditionPanelFactory} implementation.
 * Override {@link #createConditionPanel(ColumnConditionModel)} to provide custom condition panels.
 */
public class EntityConditionPanelFactory implements ConditionPanelFactory {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConditionPanelFactory.class);

  private final EntityTableConditionModel tableConditionModel;
  private final EntityComponents entityComponents;

  /**
   * Instantiates a new {@link EntityConditionPanelFactory}
   * @param tableConditionModel the table condition model
   */
  public EntityConditionPanelFactory(EntityTableConditionModel tableConditionModel) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
    this.entityComponents = new EntityComponents(tableConditionModel.getEntityDefinition());
  }

  @Override
  public final <T> ColumnConditionPanel<?, T> createConditionPanel(TableColumn column) {
    ColumnConditionModel<Attribute<T>, T> conditionModel = (ColumnConditionModel<Attribute<T>, T>)
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
  protected <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createConditionPanel(ColumnConditionModel<C, T> conditionModel) {
    return createDefaultConditionPanel(requireNonNull(conditionModel));
  }

  /**
   * Initializes a ColumnConditionPanel for the given model, returns null
   * @param <C> the column identifier type
   * @param <T> the column value type
   * @param conditionModel the {@link ColumnConditionModel} for which to create a condition panel
   * @return a ColumnConditionPanel based on the given model
   */
  protected final <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createDefaultConditionPanel(ColumnConditionModel<C, T> conditionModel) {
    ColumnConditionPanel.BoundFieldFactory boundFieldFactory;
    if (conditionModel instanceof ForeignKeyConditionModel) {
      boundFieldFactory = new ForeignKeyBoundFieldFactory((ForeignKeyConditionModel) conditionModel, entityComponents);
    }
    else if (entityComponents.supports(conditionModel.getColumnIdentifier())) {
      boundFieldFactory = new AttributeBoundFieldFactory<>(conditionModel, entityComponents, conditionModel.getColumnIdentifier());
    }
    else {
      return null;
    }
    try {
      return new ColumnConditionPanel<>(conditionModel, ToggleAdvancedButton.NO, boundFieldFactory);
    }
    catch (Exception e) {
      LOG.error("Unable to create AttributeConditionPanel for attribute: " + conditionModel.getColumnIdentifier(), e);
      return null;
    }
  }

  private static final class ForeignKeyBoundFieldFactory implements ColumnConditionPanel.BoundFieldFactory {

    private final EntityComponents entityComponents;
    private final ColumnConditionModel<ForeignKey, Entity> model;

    private ForeignKeyBoundFieldFactory(ColumnConditionModel<ForeignKey, Entity> model, EntityComponents entityComponents) {
      this.model = model;
      this.entityComponents = entityComponents;
    }

    @Override
    public JComponent createEqualField() {
      return Sizes.setPreferredHeight(createForeignKeyField(), TextComponents.getPreferredTextFieldHeight());
    }

    @Override
    public Optional<JComponent> createUpperBoundField() {
      return Optional.empty();
    }

    @Override
    public Optional<JComponent> createLowerBoundField() {
      return Optional.empty();
    }

    private JComponent createForeignKeyField() {
      if (model instanceof SwingForeignKeyConditionModel) {
        SwingEntityComboBoxModel comboBoxModel = ((SwingForeignKeyConditionModel) model).getEntityComboBoxModel();

        return entityComponents.foreignKeyComboBox(model.getColumnIdentifier(), comboBoxModel)
                .completionMode(Completion.Mode.MAXIMUM_MATCH)
                .onSetVisible(comboBox -> comboBoxModel.refresh())
                .build();
      }

      EntitySearchModel searchModel = ((ForeignKeyConditionModel) model).getEntitySearchModel();

      return entityComponents.foreignKeySearchField(model.getColumnIdentifier(), searchModel).build();
    }
  }

  private static final class AttributeBoundFieldFactory<C extends Attribute<T>, T> implements ColumnConditionPanel.BoundFieldFactory {

    private final ColumnConditionModel<C, T> conditionModel;
    private final EntityComponents inputComponents;
    private final Attribute<T> attribute;

    private AttributeBoundFieldFactory(ColumnConditionModel<C, T> conditionModel,
                                       EntityComponents inputComponents,
                                       Attribute<T> attribute) {
      this.conditionModel = requireNonNull(conditionModel);
      this.inputComponents = inputComponents;
      this.attribute = requireNonNull(attribute);
    }

    @Override
    public JComponent createEqualField() {
      return inputComponents.component(attribute)
              .linkedValue(conditionModel.getEqualValueSet().value())
              .onBuild(AttributeBoundFieldFactory::configureComponent)
              .build();
    }

    @Override
    public Optional<JComponent> createUpperBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return Optional.empty();//no upper bound field required for booleans
      }

      return Optional.of(inputComponents.component(attribute)
              .linkedValue(conditionModel.getUpperBoundValue())
              .onBuild(AttributeBoundFieldFactory::configureComponent)
              .build());
    }

    @Override
    public Optional<JComponent> createLowerBoundField() {
      if (conditionModel.getTypeClass().equals(Boolean.class)) {
        return Optional.empty();//no lower bound field required for booleans
      }

      return Optional.of(inputComponents.component(attribute)
              .linkedValue(conditionModel.getLowerBoundValue())
              .onBuild(AttributeBoundFieldFactory::configureComponent)
              .build());
    }

    private static JComponent configureComponent(JComponent component) {
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
