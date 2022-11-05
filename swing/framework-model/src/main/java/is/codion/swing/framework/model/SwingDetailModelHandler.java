/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.model.DefaultDetailModelHandler;

/**
 * A Swing {@link DefaultDetailModelHandler} implementation.
 */
public class SwingDetailModelHandler extends DefaultDetailModelHandler<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   */
  public SwingDetailModelHandler(SwingEntityModel detailModel) {
    super(detailModel);
  }
}
