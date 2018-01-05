/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityApplicationModel;

/**
 * A JavaFX implementation of {@link DefaultEntityApplicationModel}
 */
public class FXEntityApplicationModel extends DefaultEntityApplicationModel<FXEntityModel> {

  /**
   * Instantiates a new {@link FXEntityApplicationModel}
   * @param connectionProvider the connection provider
   */
  public FXEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
