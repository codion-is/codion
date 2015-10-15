/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

public class EntityModel {

  private final EntityEditModel editModel;
  private final ObservableEntityList entityList;

  public EntityModel(final EntityEditModel editModel, final ObservableEntityList entityList) {
    this.editModel = editModel;
    this.entityList = entityList;
  }

  public EntityEditModel getEditModel() {
    return editModel;
  }

  public ObservableEntityList getEntityList() {
    return entityList;
  }
}
