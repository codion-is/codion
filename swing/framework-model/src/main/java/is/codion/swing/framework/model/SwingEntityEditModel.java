/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.Refreshable;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultEntityEditModel;
import is.codion.framework.model.EntityEditModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel},
   * @see is.codion.common.model.Refreshable
   */
  private final Map<Attribute<?>, FilteredComboBoxModel<?>> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getValidator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider, final EntityValidator validator) {
    super(entityType, connectionProvider, validator);
  }

  @Override
  public void clear() {
    clearComboBoxModels();
  }

  /**
   * Refreshes all combobox models
   */
  public final void refreshComboBoxModels() {
    for (final Refreshable comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.refresh();
    }
  }

  /**
   * Clears all combobox models
   */
  public final void clearComboBoxModels() {
    for (final Refreshable comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.clear();
    }
  }

  /**
   * Returns a {@link SwingEntityComboBoxModel} for the given foreign key attribute. If one does not exist it is created.
   * @param foreignKeyAttribute the foreign key attribute
   * @return a {@link SwingEntityComboBoxModel} based on the entity referenced by the given foreign key attribute
   * @see #createForeignKeyComboBoxModel(ForeignKeyProperty)
   */
  public final SwingEntityComboBoxModel getForeignKeyComboBoxModel(final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    return getForeignKeyComboBoxModel(getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute));
  }

  /**
   * Returns a {@link SwingEntityComboBoxModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyProperty the foreign key property
   * @return a {@link SwingEntityComboBoxModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyComboBoxModel(ForeignKeyProperty)
   */
  public final SwingEntityComboBoxModel getForeignKeyComboBoxModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKyProperty");
    SwingEntityComboBoxModel comboBoxModel = (SwingEntityComboBoxModel) comboBoxModels.get(foreignKeyProperty.getAttribute());
    if (comboBoxModel == null) {
      comboBoxModel = createForeignKeyComboBoxModel(foreignKeyProperty);
      comboBoxModels.put(foreignKeyProperty.getAttribute(), comboBoxModel);
    }

    return comboBoxModel;
  }

  /**
   * Returns a {@link FilteredComboBoxModel} for the given attribute,
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link FilteredComboBoxModel} for the given attribute
   */
  public final <T> FilteredComboBoxModel<T> getComboBoxModel(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    FilteredComboBoxModel<T> comboBoxModel = (FilteredComboBoxModel<T>) comboBoxModels.get(attribute);
    if (comboBoxModel == null) {
      comboBoxModel = createComboBoxModel(attribute);
      comboBoxModels.put(attribute, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /**
   * @param attribute the attribute
   * @return true if this edit model contains a combobox model for the attribute
   */
  public final boolean containsComboBoxModel(final Attribute<?> attribute) {
    return comboBoxModels.containsKey(attribute);
  }

  /**
   * Creates a default {@link SwingEntityComboBoxModel} for the given property, override to provide
   * a specific {@link SwingEntityComboBoxModel} (filtered for example) for properties.
   * This method is called when creating a {@link SwingEntityComboBoxModel} for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted {@link SwingEntityComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a {@link SwingEntityComboBoxModel}
   * @return a {@link SwingEntityComboBoxModel} for the given property
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_VALUE_ITEM
   * @see Property#isNullable()
   */
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final SwingEntityComboBoxModel model = new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityType(),
            getConnectionProvider());
    if (getValidator().isNullable(getEntity(), foreignKeyProperty)) {
      model.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given attribute.
   * This default implementation returns a sorted {@link SwingPropertyComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable
   * @param attribute the attribute
   * @param <T> the value type
   * @return a combo box model based on the given attribute
   */
  public <T> SwingPropertyComboBoxModel<T> createComboBoxModel(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    final SwingPropertyComboBoxModel<T> model = new SwingPropertyComboBoxModel<>(getConnectionProvider(), attribute, null);
    model.setNullString(getValidator().isNullable(getEntity(), getEntityDefinition().getProperty(attribute)) ?
            FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get() : null);
    model.refresh();
    addEntitiesChangedListener(model::refresh);

    return model;
  }

  @Override
  public final void addForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getAttribute())) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
          for (final Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  @Override
  public final void removeForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getAttribute())) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
          final Entity selectedEntity = comboBoxModel.getSelectedValue();
          for (final Entity deletedEntity : entry.getValue()) {
            comboBoxModel.removeItem(deletedEntity);
          }
          if (comboBoxModel.isVisible(selectedEntity)) {
            comboBoxModel.setSelectedItem(selectedEntity);
          }//if the null value is selected we're fine, otherwise select topmost item
          else if (!comboBoxModel.isNullValueSelected() && comboBoxModel.getSize() > 0) {
            comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(0));
          }
          else {
            comboBoxModel.setSelectedItem(null);
          }
        }
        clearForeignKeyReferences(foreignKeyProperty.getAttribute(), entry.getValue());
      }
    }
  }

  @Override
  protected void refreshDataModels() {
    refreshComboBoxModels();
  }

  @Override
  protected void replaceForeignKey(final ForeignKeyProperty foreignKeyProperty, final List<Entity> entities) {
    super.replaceForeignKey(foreignKeyProperty, entities);
    if (containsComboBoxModel(foreignKeyProperty.getAttribute())) {
      final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty.getAttribute());
      entities.forEach(foreignKeyValue -> comboBoxModel.replaceItem(foreignKeyValue, foreignKeyValue));
    }
  }

  private void clearForeignKeyReferences(final Attribute<Entity> foreignKeyProperty, final List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKeyProperty))) {
        put(foreignKeyProperty, null);
      }
    });
  }
}
