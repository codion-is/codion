/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableModel;

public class SwingEntityModel extends DefaultEntityModel {

  /** Instantiates a new SwingEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityID the ID of the Entity this DefaultEntityModel represents
   * @param connectionProvider a EntityConnectionProvider
   */
  public SwingEntityModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(Util.rejectNullValue(entityID, "entityID"),
            Util.rejectNullValue(connectionProvider, "connectionProvider")));
  }

  /**
   * Instantiates a new SwingEntityModel, including a {@link DefaultEntityTableModel}
   * @param editModel the edit model
   */
  public SwingEntityModel(final EntityEditModel editModel) {
    super(editModel, new DefaultEntityTableModel(editModel.getEntityID(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new SwingEntityModel, including a default {@link SwingEntityEditModel}
   * @param tableModel the table model
   */
  public SwingEntityModel(final EntityTableModel tableModel) {
    this(tableModel.hasEditModel() ? tableModel.getEditModel() : new SwingEntityEditModel(tableModel.getEntityID(),
            tableModel.getConnectionProvider()), tableModel);
  }

  /**
   * Instantiates a new SwingEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public SwingEntityModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    super(editModel, tableModel);
  }
}
