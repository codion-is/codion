/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.model.DefaultEntityModelLink;

/**
 * A Swing {@link DefaultEntityModelLink} implementation.
 */
public class SwingEntityModelLink extends DefaultEntityModelLink<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   */
  public SwingEntityModelLink(SwingEntityModel detailModel) {
    super(detailModel);
  }
}
