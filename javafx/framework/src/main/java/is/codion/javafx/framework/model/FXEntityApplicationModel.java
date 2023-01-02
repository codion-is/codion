/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.DefaultEntityApplicationModel;

/**
 * A JavaFX implementation of {@link DefaultEntityApplicationModel}
 */
public class FXEntityApplicationModel
        extends DefaultEntityApplicationModel<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  /**
   * Instantiates a new {@link FXEntityApplicationModel}
   * @param connectionProvider the connection provider
   */
  public FXEntityApplicationModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
