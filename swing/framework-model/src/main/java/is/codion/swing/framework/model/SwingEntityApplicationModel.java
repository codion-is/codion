/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.DefaultEntityApplicationModel;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityApplicationModel}
 */
public class SwingEntityApplicationModel
        extends DefaultEntityApplicationModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * Instantiates a new {@link SwingEntityApplicationModel}
   * @param connectionProvider the connection provider
   */
  public SwingEntityApplicationModel(EntityConnectionProvider connectionProvider) {
    this(connectionProvider, null);
  }

  /**
   * Instantiates a new {@link SwingEntityApplicationModel}
   * @param connectionProvider the connection provider
   * @param version the application version
   */
  public SwingEntityApplicationModel(EntityConnectionProvider connectionProvider, Version version) {
    super(connectionProvider, version);
  }
}
