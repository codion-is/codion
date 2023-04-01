/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyDetailModelLink;

/**
 * A JavaFX {@link DefaultForeignKeyDetailModelLink} implementation.
 */
public class FXForeignKeyDetailModelLink extends DefaultForeignKeyDetailModelLink<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this link on
   */
  public FXForeignKeyDetailModelLink(FXEntityModel detailModel, ForeignKey foreignKey) {
    super(detailModel, foreignKey);
  }
}
