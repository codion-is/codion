/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyEntityModelLink;

/**
 * A Swing {@link DefaultForeignKeyEntityModelLink} implementation.
 */
public class SwingForeignKeyModelLink extends DefaultForeignKeyEntityModelLink<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this link on
   */
  public SwingForeignKeyModelLink(SwingEntityModel detailModel, ForeignKey foreignKey) {
    super(detailModel, foreignKey);
  }
}
