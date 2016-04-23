/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityModel;

import java.util.Objects;

public class FXEntityModel extends DefaultEntityModel<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  public FXEntityModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(new FXEntityEditModel(entityID, connectionProvider), new FXEntityListModel(entityID, connectionProvider));
  }

  public FXEntityModel(final FXEntityEditModel editModel) {
    this(Objects.requireNonNull(editModel), new FXEntityListModel(editModel.getEntityID(), editModel.getConnectionProvider()));
  }

  public FXEntityModel(final FXEntityListModel tableModel) {
    this(Objects.requireNonNull(tableModel).getEditModel() == null ? new FXEntityEditModel(tableModel.getEntityID(),
            tableModel.getConnectionProvider()) : tableModel.getEditModel(), tableModel);
  }

  public FXEntityModel(final FXEntityEditModel editModel, final FXEntityListModel listModel) {
    super(editModel, listModel);
  }
}
