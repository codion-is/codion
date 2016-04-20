/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.EventListener;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityEditModel;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.PropertyComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel},
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<String, Refreshable> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, Entities.getValidator(entityID));
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider, final Entity.Validator validator) {
    super(entityID, connectionProvider, validator);
  }

  /** {@inheritDoc} */
  @Override
  public void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    super.replaceForeignKeyValues(foreignKeyEntityID, foreignKeyValues);
    final List<Property.ForeignKeyProperty> foreignKeyProperties =
            Entities.getForeignKeyProperties(getEntityID(), foreignKeyEntityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
        getForeignKeyComboBoxModel(foreignKeyProperty.getPropertyID()).refresh();
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

  public final void refreshComboBoxModels() {
    for (final Refreshable comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.refresh();
    }
  }

  public final void clearComboBoxModels() {
    for (final Refreshable  comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.clear();
    }
  }

  public final EntityComboBoxModel getForeignKeyComboBoxModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, "foreignKeyPropertyID");
    return getForeignKeyComboBoxModel(Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID));
  }

  public final EntityComboBoxModel getForeignKeyComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKyProperty");
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) comboBoxModels.get(foreignKeyProperty.getPropertyID());
    if (comboBoxModel == null) {
      comboBoxModel = createForeignKeyComboBoxModel(foreignKeyProperty);
      comboBoxModels.put(foreignKeyProperty.getPropertyID(), comboBoxModel);
    }

    return comboBoxModel;
  }

  public final PropertyComboBoxModel getComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    PropertyComboBoxModel comboBoxModel = (PropertyComboBoxModel) comboBoxModels.get(propertyID);
    if (comboBoxModel == null) {
      comboBoxModel = createComboBoxModel(Entities.getColumnProperty(getEntityID(), propertyID));
      comboBoxModels.put(propertyID, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  public final boolean containsComboBoxModel(final String propertyID) {
    return comboBoxModels.containsKey(propertyID);
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
   * @see org.jminor.framework.Configuration#COMBO_BOX_NULL_VALUE_ITEM
   * @see Property#isNullable()
   */
  public EntityComboBoxModel createForeignKeyComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(),
            getConnectionProvider());
    if (getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID())) {
      model.setNullValue(EntityUtil.createToStringEntity(foreignKeyProperty.getReferencedEntityID(),
              (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM)));
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given property.
   * This default implementation returns a sorted {@link PropertyComboBoxModel} with the default nullValueItem
   * if the underlying property is nullable
   * @param property the property
   * @return a combo box model based on the given property
   */
  public PropertyComboBoxModel createComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, "property");
    final PropertyComboBoxModel model = new DefaultPropertyComboBoxModel<>(getEntityID(),
            getConnectionProvider(), property, null);
    ((FilteredComboBoxModel) model).setNullValue(getValidator().isNullable(getEntity(), property.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM) : null);
    model.refresh();
    addEntitiesChangedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        model.refresh();
      }
    });

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public void addForeignKeyValues(final List<Entity> values) {
    final Map<String, Collection<Entity>> mapped = EntityUtil.mapToEntityID(values);
    for (final Map.Entry<String, Collection<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(getEntityID(), entry.getKey())) {
        final EntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
        if (comboBoxModel != null) {
          for (final Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void removeForeignKeyValues(final List<Entity> values) {
    final Map<String, Collection<Entity>> mapped = EntityUtil.mapToEntityID(values);
    for (final Map.Entry<String, Collection<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(getEntityID(), entry.getKey())) {
        final EntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKeyProperty);
        if (comboBoxModel != null) {
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
