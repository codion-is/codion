/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityModel;

/**
 * A JavaFX {@link is.codion.framework.model.EntityEditModel} implementation
 */
public class FXEntityModel extends DefaultEntityModel<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  /**
   * Instantiates a new {@link FXEntityModel} with default {@link FXEntityEditModel}
   * and {@link FXEntityListModel} implementations
   * @param entityType the type of the entity on which to base the model
   * @param connectionProvider the connection provider
   */
  public FXEntityModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(new FXEntityListModel(entityType, connectionProvider));
  }

  /**
   * Instantiates a new {@link FXEntityModel} with a default {@link FXEntityListModel} implementation
   * @param editModel the {@link FXEntityEditModel} to use
   * @throws IllegalArgumentException in case editModel is null
   */
  public FXEntityModel(FXEntityEditModel editModel) {
    this(new FXEntityListModel(editModel));
  }

  /**
   * Instantiates a new {@link FXEntityModel}, using the edit model provided by the list model,
   * or a default {@link FXEntityEditModel} implementation if the list model does not contain an edit model.
   * @param listModel the {@link FXEntityListModel} to use
   */
  public FXEntityModel(FXEntityListModel listModel) {
    super(listModel);
  }
}
