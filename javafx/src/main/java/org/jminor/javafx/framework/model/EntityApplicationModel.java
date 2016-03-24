/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;

import java.util.ArrayList;
import java.util.List;

public class EntityApplicationModel {

  private final EntityConnectionProvider connectionProvider;
  private final List<EntityModel> entityModels = new ArrayList<>();

  public EntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public final void addEntityModel(final EntityModel entityModel) {
    if (!entityModels.contains(entityModel)) {
      entityModels.add(entityModel);
    }
  }

  public final EntityModel getEntityModel(final String entityID) {
    for (final EntityModel model : entityModels) {
      if (model.getEntityID().equals(entityID)) {
        return model;
      }
    }

    throw new IllegalArgumentException("Entity model with entityID '" + entityID + "' not found");
  }

  public final EntityModel getEntityModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("Entity model of class '" + modelClass + "' not found");
  }
}
