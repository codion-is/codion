/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityApplicationModel;

/**
 * A Swing implementation of {@link org.jminor.framework.model.EntityApplicationModel}
 */
public abstract class SwingEntityApplicationModel extends DefaultEntityApplicationModel<SwingEntityModel> {

  /**
   * Instantiates a new {@link SwingEntityApplicationModel}
   * @param connectionProvider the connectio provider
   */
  public SwingEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
