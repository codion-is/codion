/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.model.DefaultDetailModelLink;

/**
 * A JavaFX {@link DefaultDetailModelLink} implementation.
 */
public class FXDetailModelLink extends DefaultDetailModelLink<FXEntityModel, FXEntityEditModel, FXEntityListModel> {

  /**
   * @param detailModel the detail model
   */
  public FXDetailModelLink(FXEntityModel detailModel) {
    super(detailModel);
  }
}
