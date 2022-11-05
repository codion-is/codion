/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyDetailModelHandler;

/**
 * A Swing {@link DefaultForeignKeyDetailModelHandler} implementation.
 */
public class SwingForeignKeyDetailModelHandler extends DefaultForeignKeyDetailModelHandler<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this handler on
   */
  public SwingForeignKeyDetailModelHandler(SwingEntityModel detailModel, ForeignKey foreignKey) {
    super(detailModel, foreignKey);
  }
}
