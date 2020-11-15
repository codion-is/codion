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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel},
   * @see is.codion.common.model.Refreshable
   */
  private final Map<Attribute<?>, FilteredComboBoxModel<?>> comboBoxModels = new ConcurrentHashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getValidator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider, final EntityValidator validator) {
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
   * @see #createForeignKeyComboBoxModel(Attribute)
   */
  public final SwingEntityComboBoxModel getForeignKeyComboBoxModel(final Attribute<Entity> foreignKeyAttribute) {
    getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute);

    return (SwingEntityComboBoxModel) comboBoxModels.computeIfAbsent(foreignKeyAttribute,
            attribute -> createForeignKeyComboBoxModel(foreignKeyAttribute));
  }

  /**
   * Returns a {@link FilteredComboBoxModel} for the given attribute,
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link FilteredComboBoxModel} for the given attribute
   */
  public final <T> FilteredComboBoxModel<T> getComboBoxModel(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");

    return (FilteredComboBoxModel<T>) comboBoxModels.computeIfAbsent(attribute,
            theAttribute -> createComboBoxModel(attribute));
  }

  /**
   * @param attribute the attribute
   * @return true if this edit model contains a combobox model for the attribute
   */
  public final boolean containsComboBoxModel(final Attribute<?> attribute) {
    return comboBoxModels.containsKey(attribute);
  }

  /**
   * Creates a default {@link SwingEntityComboBoxModel} for the given attribute, override to provide
   * a specific {@link SwingEntityComboBoxModel} (filtered for example) for attributes.
   * This method is called when creating a {@link SwingEntityComboBoxModel} for entity attributes, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted {@link SwingEntityComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable
   * @param foreignKeyAttribute the foreign key attribute for which to create a {@link SwingEntityComboBoxModel}
   * @return a {@link SwingEntityComboBoxModel} for the given attribute
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_VALUE_ITEM
   * @see Property#isNullable()
   */
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(final Attribute<Entity> foreignKeyAttribute) {
    final ForeignKeyProperty foreignKeyProperty = getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute);
    final SwingEntityComboBoxModel model =
            new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityType(), getConnectionProvider());
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
    final Map<EntityType<?>, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType<?>, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getAttribute())) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty.getAttribute());
          for (final Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  @Override
  public final void removeForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType<?>, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType<?>, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getAttribute())) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty.getAttribute());
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
