/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.model;

import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.model.DefaultEntityApplicationModel;

/**
 * A Swing implementation of {@link dev.codion.framework.model.EntityApplicationModel}
 */
public class SwingEntityApplicationModel extends DefaultEntityApplicationModel<SwingEntityModel> {

  /**
   * Instantiates a new {@link SwingEntityApplicationModel}
   * @param connectionProvider the connectio provider
   */
  public SwingEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
