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

  private final EntityEditModel editModel;
  private final EntityListModel tableModell;

  private final List<EntityModel> detailModels = new ArrayList<>();

  public EntityModel(final EntityEditModel editModel, final EntityListModel tableModel) {
    Objects.requireNonNull(editModel);
    this.editModel = editModel;
    this.tableModell = tableModel;
    if (this.tableModell != null) {
      this.tableModell.setEditModel(editModel);
    }
    bindEvents();
  }

  public final String getEntityID() {
    return editModel.getEntityID();
  }

  public final EntityEditModel getEditModel() {
    return editModel;
  }

  public final EntityListModel getTableModell() {
    return tableModell;
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

  private void bindEvents() {
    if (tableModell != null) {
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
    if (tableModell != null) {
      tableModell.addSelectionModelSetListener(selectionModel ->
              selectionModel.getSelectedItems().addListener((ListChangeListener<Entity>) c -> {
        try {
          for (final EntityModel detailModel : detailModels) {
            detailModel.initialize(getEntityID(), tableModell.getSelectionModel().getSelectedItems());
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
    tableModell.filterBy(foreignKeyProperties.get(0), foreignKeyEntities);
  }
}
