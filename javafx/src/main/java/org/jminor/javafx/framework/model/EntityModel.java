/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EntityModel {

  private final EntityEditModel editModel;
  private final EntityListModel tableModel;

  private final List<EntityModel> detailModels = new ArrayList<>();

  public EntityModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(new EntityEditModel(entityID, connectionProvider));
  }

  public EntityModel(final EntityEditModel editModel) {
    this(Objects.requireNonNull(editModel), new EntityListModel(editModel.getEntityID(), editModel.getConnectionProvider()));
  }

  public EntityModel(final EntityListModel tableModel) {
    this(Objects.requireNonNull(tableModel).getEditModel() == null ? new EntityEditModel(tableModel.getEntityID(),
            tableModel.getConnectionProvider()) : tableModel.getEditModel(), tableModel);
  }

  public EntityModel(final EntityEditModel editModel, final EntityListModel tableModel) {
    Objects.requireNonNull(editModel);
    this.editModel = editModel;
    this.tableModel = tableModel;
    if (this.tableModel != null) {
      this.tableModel.setEditModel(editModel);
    }
    bindEvents();
  }

  public final String getEntityID() {
    return editModel.getEntityID();
  }

  public final EntityEditModel getEditModel() {
    return editModel;
  }

  public final EntityListModel getTableModel() {
    return tableModel;
  }

  public final void addDetailModel(final EntityModel entityModel) {
    detailModels.add(entityModel);
  }

  public final EntityModel getDetailModel(final String entityID) {
    for (final EntityModel model : detailModels) {
      if (model.getEntityID().equals(entityID)) {
        return model;
      }
    }

    throw new IllegalArgumentException("Detail model with entityID '" + entityID + "' not found");
  }

  public final EntityModel getDetailModel(final Class<? extends EntityModel> detailModelClass) {
    for (final EntityModel model : detailModels) {
      if (model.getClass().equals(detailModelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("Detail model of class '" + detailModelClass + "' not found");
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getEntityID();
  }

  /**
   * By default this method sets the foreign key value in the edit model if the entity is new, using the first item in {@code foreignKeyValues}.
   * @param foreignKeyProperty the foreign key referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model
   */
  protected void handleInitialization(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (editModel.isEntityNew() && !Util.nullOrEmpty(foreignKeyValues)) {
      editModel.setValue(foreignKeyProperty.getPropertyID(), foreignKeyValues.get(0));
    }
  }

  private void bindEvents() {
    if (tableModel == null) {
      editModel.addEntitySetListener(entity -> {
        try {
          for (final EntityModel detailModel : detailModels) {
            detailModel.initialize(getEntityID(), entity == null ? Collections.emptyList() : Collections.singletonList(entity));
          }
        }
        catch (final DatabaseException e) {
          throw new RuntimeException(e);
        }
      });
    }
    else {
      tableModel.addSelectionModelSetListener(selectionModel ->
              selectionModel.getSelectedItems().addListener((ListChangeListener<Entity>) c -> {
                try {
                  for (final EntityModel detailModel : detailModels) {
                    detailModel.initialize(getEntityID(), tableModel.getSelectionModel().getSelectedItems());
                  }
                }
                catch (final DatabaseException e) {
                  throw new RuntimeException(e);
                }
              }));
    }
  }

  private void initialize(final String masterEntityID, final List<Entity> foreignKeyEntities) throws DatabaseException {
    final List<Property.ForeignKeyProperty> foreignKeyProperties =
            Entities.getForeignKeyProperties(getEntityID(), masterEntityID);
    if (!foreignKeyProperties.isEmpty()) {
      initialize(foreignKeyProperties.get(0), foreignKeyEntities);
    }
  }

  private void initialize(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (tableModel != null) {
      tableModel.filterBy(foreignKeyProperty, foreignKeyValues);
    }
    handleInitialization(foreignKeyProperty, foreignKeyValues);
  }
}
