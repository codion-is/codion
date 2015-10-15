/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.domain.Entity;

import java.util.List;

public class EntityModel {

  private final String entityID;
  private final EntityEditModel editModel;
  private final ObservableEntityList entityList;

  public EntityModel(final EntityEditModel editModel, final ObservableEntityList entityList) {
    this.editModel = editModel;
    this.entityList = entityList;
    this.entityID = editModel.getEntityID();
    bindEvents();
  }

  private void bindEvents() {
    editModel.addInsertListener(inserted -> handleInsert(inserted));
    editModel.addUpdateListener(updated -> handleUpdate(updated));
    editModel.addDeleteListener(deleted -> handleDelete(deleted));
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
