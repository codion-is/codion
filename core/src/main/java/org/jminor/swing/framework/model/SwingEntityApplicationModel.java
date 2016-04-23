/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityApplicationModel;

public abstract class SwingEntityApplicationModel extends DefaultEntityApplicationModel<SwingEntityModel> {

  public SwingEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
