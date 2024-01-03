/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntitySearchConditionModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.BoundFieldFactory;
import is.codion.swing.common.ui.component.text.TextComponents;
import is.codion.swing.framework.model.EntityComboBoxConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComponents;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link ColumnConditionPanel.Factory} implementation.
 * Override {@link #createConditionPanel(ColumnConditionModel)} to provide custom condition panels.
 */
public class EntityConditionPanelFactory implements ColumnConditionPanel.Factory<Attribute<?>> {

  private final EntityComponents entityComponents;

  /**
   * Instantiates a new {@link EntityConditionPanelFactory}
   * @param entityDefinition the entity definition
   */
  public EntityConditionPanelFactory(EntityDefinition entityDefinition) {
    this(new EntityComponents(entityDefinition));
  }

  /**
   * Instantiates a new {@link EntityConditionPanelFactory}
   * @param entityComponents the {@link EntityComponents} instance to use when creating bound fields
   */
  public EntityConditionPanelFactory(EntityComponents entityComponents)  {
    this.entityComponents = requireNonNull(entityComponents);
  }

  @Override
  public <T> Optional<ColumnConditionPanel<Attribute<?>, T>> createConditionPanel(ColumnConditionModel<Attribute<?>, T> conditionModel) {
    requireNonNull(conditionModel);
    BoundFieldFactory boundFieldFactory = createBoundFieldFactory(conditionModel);
    if (boundFieldFactory != null) {
      return columnConditionPanel(conditionModel, boundFieldFactory);
    }

    return Optional.empty();
  }

  private <T> BoundFieldFactory createBoundFieldFactory(ColumnConditionModel<? extends Attribute<?>, T> conditionModel) {
    if (conditionModel.columnIdentifier() instanceof ForeignKey) {
      return new ForeignKeyBoundFieldFactory((ColumnConditionModel<ForeignKey, Entity>) conditionModel, entityComponents);
    }
    else if (entityComponents.supports(conditionModel.columnIdentifier())) {
      return new AttributeBoundFieldFactory<>(conditionModel, entityComponents, (Attribute<T>) conditionModel.columnIdentifier());
    }

    return null;
  }

  private static final class ForeignKeyBoundFieldFactory implements BoundFieldFactory {

    private final EntityComponents entityComponents;
    private final ColumnConditionModel<ForeignKey, Entity> model;

    private ForeignKeyBoundFieldFactory(ColumnConditionModel<ForeignKey, Entity> model, EntityComponents entityComponents) {
      this.model = model;
      this.entityComponents = entityComponents;
    }

    @Override
    public boolean supportsType(Class<?> columnClass) {
      return Entity.class.isAssignableFrom(requireNonNull(columnClass));
    }

    @Override
    public JComponent createEqualField() {
      return Sizes.setPreferredHeight(createForeignKeyField(), TextComponents.preferredTextFieldHeight());
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
      if (model instanceof EntitySearchConditionModel) {
        EntitySearchModel searchModel = ((EntitySearchConditionModel) model).searchModel();

        return entityComponents.foreignKeySearchField(model.columnIdentifier(), searchModel).build();
      }
      if (model instanceof EntityComboBoxConditionModel) {
        EntityComboBoxModel comboBoxModel = ((EntityComboBoxConditionModel) model).comboBoxModel();

        return entityComponents.foreignKeyComboBox(model.columnIdentifier(), comboBoxModel)
                .completionMode(Completion.Mode.MAXIMUM_MATCH)
                .onSetVisible(comboBox -> comboBoxModel.refresh())
                .build();
      }

      throw new IllegalArgumentException("Unknown foreign key condition model type: " + model);
    }
  }

  private static final class AttributeBoundFieldFactory<T> implements BoundFieldFactory {

    private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
            Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
            BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
            LocalDateTime.class, OffsetDateTime.class);

    private final ColumnConditionModel<? extends Attribute<?>, T> conditionModel;
    private final EntityComponents inputComponents;
    private final Attribute<T> attribute;

    private AttributeBoundFieldFactory(ColumnConditionModel<? extends Attribute<?>, T> conditionModel,
                                       EntityComponents inputComponents,
                                       Attribute<T> attribute) {
      this.conditionModel = requireNonNull(conditionModel);
      this.inputComponents = inputComponents;
      this.attribute = requireNonNull(attribute);
    }

    @Override
    public boolean supportsType(Class<?> columnClass) {
      return SUPPORTED_TYPES.contains(requireNonNull(columnClass));
    }

    @Override
    public JComponent createEqualField() {
      return inputComponents.component(attribute)
              .linkedValue(conditionModel.equalValues().value())
              .onBuild(AttributeBoundFieldFactory::configureComponent)
              .build();
    }

    @Override
    public Optional<JComponent> createUpperBoundField() {
      if (conditionModel.columnClass().equals(Boolean.class)) {
        return Optional.empty();//no upper bound field required for booleans
      }

      return Optional.of(inputComponents.component(attribute)
              .linkedValue(conditionModel.upperBoundValue())
              .onBuild(AttributeBoundFieldFactory::configureComponent)
              .build());
    }

    @Override
    public Optional<JComponent> createLowerBoundField() {
      if (conditionModel.columnClass().equals(Boolean.class)) {
        return Optional.empty();//no lower bound field required for booleans
      }

      return Optional.of(inputComponents.component(attribute)
              .linkedValue(conditionModel.lowerBoundValue())
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
