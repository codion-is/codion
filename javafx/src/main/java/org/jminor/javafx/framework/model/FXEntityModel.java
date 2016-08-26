/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityModel;

import java.util.Objects;

/**
 * A JavaFX {@link org.jminor.framework.model.EntityEditModel} implementation
 */
public class FXEntityModel extends DefaultEntityModel<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  /**
   * Instantiates a new {@link FXEntityModel} with default {@link FXEntityEditModel}
   * and {@link FXEntityListModel} implementations
   * @param entityID the ID of the entity on which to base the model
   * @param connectionProvider the connection provider
   */
  public FXEntityModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(new FXEntityEditModel(entityID, connectionProvider), new FXEntityListModel(entityID, connectionProvider));
  }

  /**
   * Instantiates a new {@link FXEntityModel} with a default {@link FXEntityListModel} implementation
   * @param editModel the {@link FXEntityEditModel} to use
   */
  public FXEntityModel(final FXEntityEditModel editModel) {
    this(Objects.requireNonNull(editModel), new FXEntityListModel(editModel.getEntityID(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new {@link FXEntityModel} with a default {@link FXEntityEditModel} implementation
   * @param tableModel the {@link FXEntityListModel} to use
   */
  public FXEntityModel(final FXEntityListModel tableModel) {
    this(Objects.requireNonNull(tableModel).getEditModel() == null ? new FXEntityEditModel(tableModel.getEntityID(),
            tableModel.getConnectionProvider()) : tableModel.getEditModel(), tableModel);
  }

  /**
   * Instantiates a new {@link FXEntityModel}
   * @param editModel the {@link FXEntityEditModel} to use
   * @param tableModel the {@link FXEntityListModel} to use
   */
  public FXEntityModel(final FXEntityEditModel editModel, final FXEntityListModel listModel) {
    super(editModel, listModel);
  }
}
