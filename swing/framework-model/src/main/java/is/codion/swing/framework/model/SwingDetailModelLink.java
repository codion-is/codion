/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.model.DefaultDetailModelLink;

/**
 * A Swing {@link DefaultDetailModelLink} implementation.
 */
public class SwingDetailModelLink extends DefaultDetailModelLink<SwingEntityModel, SwingEntityEditModel, SwingEntityTableModel> {

  /**
   * @param detailModel the detail model
   */
  public SwingDetailModelLink(SwingEntityModel detailModel) {
    super(detailModel);
  }
}
