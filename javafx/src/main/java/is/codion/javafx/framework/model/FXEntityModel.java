/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityModel;

import static java.util.Objects.requireNonNull;

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
  public FXEntityModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    this(new FXEntityEditModel(entityType, connectionProvider), new FXEntityListModel(entityType, connectionProvider));
  }

  /**
   * Instantiates a new {@link FXEntityModel} with a default {@link FXEntityListModel} implementation
   * @param editModel the {@link FXEntityEditModel} to use
   * @throws IllegalArgumentException in case editModel is null
   */
  public FXEntityModel(final FXEntityEditModel editModel) {
    this(requireNonNull(editModel), new FXEntityListModel(editModel.getEntityType(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new {@link FXEntityModel}, using the edit model provided by the list model,
   * or a default {@link FXEntityEditModel} implementation if the list model does not contain an edit model.
   * @param listModel the {@link FXEntityListModel} to use
   */
  public FXEntityModel(final FXEntityListModel listModel) {
    this(requireNonNull(listModel).getEditModel() == null ? new FXEntityEditModel(listModel.getEntityType(),
            listModel.getConnectionProvider()) : listModel.getEditModel(), listModel);
  }

  /**
   * Instantiates a new {@link FXEntityModel}
   * @param editModel the {@link FXEntityEditModel} to use
   * @param listModel the {@link FXEntityListModel} to use
   * @throws IllegalArgumentException in case editModel is null
   */
  public FXEntityModel(final FXEntityEditModel editModel, final FXEntityListModel listModel) {
    super(editModel, listModel);
  }
}
