/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.DefaultEntityModel;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link is.codion.framework.model.EntityModel}
 */
public class SwingEntityModel extends DefaultEntityModel<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * Instantiates a new SwingEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityType the type of the entity this DefaultEntityModel represents
   * @param connectionProvider a EntityConnectionProvider
   */
  public SwingEntityModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(requireNonNull(entityType, "entityType"),
            requireNonNull(connectionProvider, "connectionProvider")));
  }

  /**
   * Instantiates a new SwingEntityModel, including a {@link SwingEntityTableModel}
   * @param editModel the edit model
   */
  public SwingEntityModel(final SwingEntityEditModel editModel) {
    super(editModel, new SwingEntityTableModel(editModel.getEntityType(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new SwingEntityModel, including a default {@link SwingEntityEditModel}
   * @param tableModel the table model
   */
  public SwingEntityModel(final SwingEntityTableModel tableModel) {
    this(tableModel.hasEditModel() ? tableModel.getEditModel() : new SwingEntityEditModel(tableModel.getEntityType(),
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
