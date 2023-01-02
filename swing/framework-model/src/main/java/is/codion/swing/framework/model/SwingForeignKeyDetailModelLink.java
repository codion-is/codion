/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyDetailModelLink;

/**
 * A Swing {@link DefaultForeignKeyDetailModelLink} implementation.
 */
public class SwingForeignKeyDetailModelLink extends DefaultForeignKeyDetailModelLink<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this link on
   */
  public SwingForeignKeyDetailModelLink(SwingEntityModel detailModel, ForeignKey foreignKey) {
    super(detailModel, foreignKey);
  }
}
