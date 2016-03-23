/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javafx.collections.ListChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EntityModel {

  private final String entityID;
  private final EntityEditModel editModel;
  private final ObservableEntityList entityList;

  private final List<EntityModel> detailModels = new ArrayList<>();

  public EntityModel(final EntityEditModel editModel, final ObservableEntityList entityList) {
    Objects.requireNonNull(editModel);
    this.editModel = editModel;
    this.entityList = entityList;
    if (entityList != null && editModel != null) {
      entityList.setEditModel(editModel);
    }
    this.entityID = editModel.getEntityID();
    bindEvents();
  }

  public final void addDetailModel(final EntityModel entityModel) {
    detailModels.add(entityModel);
  }

  private void bindEvents() {
    editModel.addInsertListener(this::handleInsert);
    editModel.addUpdateListener(this::handleUpdate);
    editModel.addDeleteListener(this::handleDelete);
    if (entityList != null) {
      editModel.addEntitySetListener(entity -> {
        try {
          for (final EntityModel detailModel : detailModels) {
            detailModel.initialize(getEntityID(), Collections.singletonList(entity));
          }
        }
        catch (final DatabaseException e) {
          throw new RuntimeException(e);
        }
      });
    }
    if (entityList != null) {
      entityList.addSelectionModelSetListener(selectionModel ->
              selectionModel.getSelectedItems().addListener((ListChangeListener<Entity>) c -> {
        try {
          for (final EntityModel detailModel : detailModels) {
            detailModel.initialize(getEntityID(), entityList.getSelectionModel().getSelectedItems());
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
    editModel.setValue(foreignKeyProperties.get(0).getPropertyID(), foreignKeyEntities.get(0));
    entityList.filterBy(foreignKeyProperties.get(0), foreignKeyEntities);
  }

  private void handleInsert(final List<Entity> inserted) {
    entityList.addAll(0, inserted);
  }

  private void handleUpdate(final List<Entity> updated) {
    for (final Entity entity : updated) {
      final int index = entityList.indexOf(entity);
      if (index != -1) {
        entityList.set(index, entity);
      }
    }
  }

  private void handleDelete(final List<Entity> deleted) {
    entityList.removeAll(deleted);
  }

  public String getEntityID() {
    return entityID;
  }

  public EntityEditModel getEditModel() {
    return editModel;
  }

  public ObservableEntityList getEntityList() {
    return entityList;
  }
}
