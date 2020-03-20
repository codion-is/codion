/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.model.DefaultEntityModel;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link org.jminor.framework.model.EntityModel}
 */
public class SwingEntityModel extends DefaultEntityModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * Instantiates a new SwingEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityId the ID of the Entity this DefaultEntityModel represents
   * @param connectionProvider a EntityConnectionProvider
   */
  public SwingEntityModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(requireNonNull(entityId, "entityId"),
            requireNonNull(connectionProvider, "connectionProvider")));
  }

  /**
   * Instantiates a new SwingEntityModel, including a {@link SwingEntityTableModel}
   * @param editModel the edit model
   */
  public SwingEntityModel(final SwingEntityEditModel editModel) {
    super(editModel, new SwingEntityTableModel(editModel.getEntityId(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new SwingEntityModel, including a default {@link SwingEntityEditModel}
   * @param tableModel the table model
   */
  public SwingEntityModel(final SwingEntityTableModel tableModel) {
    this(tableModel.hasEditModel() ? tableModel.getEditModel() : new SwingEntityEditModel(tableModel.getEntityId(),
            tableModel.getConnectionProvider()), tableModel);
  }

  /**
   * Instantiates a new SwingEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public SwingEntityModel(final SwingEntityEditModel editModel, final SwingEntityTableModel tableModel) {
    super(editModel, tableModel);
  }
}
