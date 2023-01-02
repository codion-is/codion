/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.DefaultEntityApplicationModel;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityApplicationModel}
 */
public class SwingEntityApplicationModel
        extends DefaultEntityApplicationModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * Instantiates a new {@link SwingEntityApplicationModel}
   * @param connectionProvider the connectio provider
   */
  public SwingEntityApplicationModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }
}
