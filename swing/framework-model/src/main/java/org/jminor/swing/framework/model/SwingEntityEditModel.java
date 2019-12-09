/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Refreshable;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.DefaultEntityEditModel;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel},
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<String, FilteredComboBoxModel> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, connectionProvider.getDomain().getDefinition(entityId).getValidator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider, final Entity.Validator validator) {
    super(entityId, connectionProvider, validator);
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {
    super.replaceForeignKeyValues(foreignKeyEntityId, foreignKeyValues);
    final List<ForeignKeyProperty> foreignKeyProperties =
            getEntityDefinition().getForeignKeyReferences(foreignKeyEntityId);
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (containsComboBoxModel(foreignKeyProperty.getPropertyId())) {
        getForeignKeyComboBoxModel(foreignKeyProperty.getPropertyId()).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void refreshDataModels() {
    refreshComboBoxModels();
  }

  /** {@inheritDoc} */
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
   * Returns a {@link EntityComboBoxModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyPropertyId the ID of the foreign key property
   * @return a {@link EntityComboBoxModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyComboBoxModel(ForeignKeyProperty)
   */
  public final EntityComboBoxModel getForeignKeyComboBoxModel(final String foreignKeyPropertyId) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    return getForeignKeyComboBoxModel(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  /**
   * Returns a {@link EntityComboBoxModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyProperty the foreign key property
   * @return a {@link EntityComboBoxModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyComboBoxModel(ForeignKeyProperty)
   */
  public final EntityComboBoxModel getForeignKeyComboBoxModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKyProperty");
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) comboBoxModels.get(foreignKeyProperty.getPropertyId());
    if (comboBoxModel == null) {
      comboBoxModel = createForeignKeyComboBoxModel(foreignKeyProperty);
      comboBoxModels.put(foreignKeyProperty.getPropertyId(), comboBoxModel);
    }

    return comboBoxModel;
  }

  /**
   * Returns a {@link FilteredComboBoxModel} for the given property,
   * @param propertyId the property ID
   * @return a {@link FilteredComboBoxModel} for the given property
   */
  public final FilteredComboBoxModel getComboBoxModel(final String propertyId) {
    requireNonNull(propertyId, "propertyId");
    FilteredComboBoxModel comboBoxModel = comboBoxModels.get(propertyId);
    if (comboBoxModel == null) {
      comboBoxModel = createComboBoxModel(getEntityDefinition().getColumnProperty(propertyId));
      comboBoxModels.put(propertyId, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /**
   * @param propertyId the ID of the property
   * @return true if this edit model contains a combobox model for the property
   */
  public final boolean containsComboBoxModel(final String propertyId) {
    return comboBoxModels.containsKey(propertyId);
  }

  /**
   * Creates a default {@link EntityComboBoxModel} for the given property, override to provide
   * a specific {@link EntityComboBoxModel} (filtered for example) for properties.
   * This method is called when creating a {@link EntityComboBoxModel} for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted {@link EntityComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param foreignKeyProperty the foreign key property for which to create a {@link EntityComboBoxModel}
   * @return a {@link EntityComboBoxModel} for the given property
   * @see EntityEditModel#COMBO_BOX_NULL_VALUE_ITEM
   * @see Property#isNullable()
   */
  public EntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel model = new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityId(),
            getConnectionProvider());
    if (getValidator().isNullable(getEntity(), foreignKeyProperty)) {
      model.setNullValue(getDomain().createToStringEntity(foreignKeyProperty.getForeignEntityId(),
              EntityEditModel.COMBO_BOX_NULL_VALUE_ITEM.get()));
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given property.
   * This default implementation returns a sorted {@link SwingPropertyComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param property the property
   * @return a combo box model based on the given property
   */
  public SwingPropertyComboBoxModel createComboBoxModel(final ColumnProperty property) {
    requireNonNull(property, "property");
    final SwingPropertyComboBoxModel model = new SwingPropertyComboBoxModel(getEntityId(),
            getConnectionProvider(), property, null);
    model.setNullValue(getValidator().isNullable(getEntity(), property) ?
            EntityEditModel.COMBO_BOX_NULL_VALUE_ITEM.get() : null);
    model.refresh();
    addEntitiesChangedListener(model::refresh);

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public final void addForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> mapped = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty :
              getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getPropertyId())) {
          final EntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
          for (final Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> mapped = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty :
              getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        if (containsComboBoxModel(foreignKeyProperty.getPropertyId())) {
          final EntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
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
      }
    }
  }
}
