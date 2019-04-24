/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityEditModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A JavaFX implementation of {@link org.jminor.framework.model.EntityEditModel}
 */
public class FXEntityEditModel extends DefaultEntityEditModel {

  private final Map<Property.ForeignKeyProperty, FXEntityListModel> foreignKeyListModels = new HashMap<>();

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public FXEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    super(entityId, connectionProvider);
  }

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link FXEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public FXEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider,
                           final Entity.Validator validator) {
    super(entityId, connectionProvider, validator);
  }

  /**
   * Returns a {@link FXEntityListModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyPropertyId the ID of the foreign key property
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyListModel(Property.ForeignKeyProperty)
   */
  public final FXEntityListModel getForeignKeyListModel(final String foreignKeyPropertyId) {
    return getForeignKeyListModel(getDomain().getForeignKeyProperty(getEntityId(), foreignKeyPropertyId));
  }

  /**
   * Returns a {@link FXEntityListModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyProperty the foreign key property
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyListModel(Property.ForeignKeyProperty)
   */
  public final FXEntityListModel getForeignKeyListModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty);

    return foreignKeyListModels.computeIfAbsent(foreignKeyProperty, k -> createForeignKeyListModel(foreignKeyProperty));
  }

  /**
   * Creates a {@link FXEntityListModel} based on the given foreign key property
   * @param foreignKeyProperty the foreign key property
   * @return a new {@link FXEntityListModel} based on the given property
   */
  public FXEntityListModel createForeignKeyListModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty);
    return new FXEntityListModel(foreignKeyProperty.getForeignEntityId(), getConnectionProvider());
    //todo
//    if (getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyId())) {
//      model.setNullValue(Domain.createToStringEntity(foreignKeyProperty.getForeignEntityId(),
//              (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM)));
//    }
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    foreignKeyListModels.values().forEach(FXEntityListModel::clear);
  }

  /**
   * Adds the given foreign key values to respective {@link FXEntityListModel}s.
   * @param values the values
   */
  @Override
  public void addForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> mapped = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : getDomain().getForeignKeyProperties(getEntityId(), entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
        if (listModel != null) {
          listModel.addAll(entry.getValue());
        }
      }
    }
  }

  /**
   * Removes the given foreign key values from respective {@link FXEntityListModel}s.
   * @param values the values
   */
  @Override
  public void removeForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> mapped = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : getDomain().getForeignKeyProperties(getEntityId(), entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
        if (listModel != null) {
          listModel.removeAll(entry.getValue());
          //todo
//          final Entity selectedEntity = listModel.getSelectionModel().getSelectedItem();
//          if (listModel.isVisible(selectedEntity)) {
//            listModel.setSelectedItem(selectedEntity);
//          }//if the null value is selected we're fine, otherwise select topmost item
//          else if (!listModel.isNullValueSelected() && listModel.getSize() > 0) {
//            listModel.setSelectedItem(listModel.getElementAt(0));
//          }
//          else {
//            listModel.setSelectedItem(null);
//          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }
}
