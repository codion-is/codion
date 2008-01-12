/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.ui.EntityPanel;

import javax.swing.JPanel;

public class ColumnConstraintPanel extends EntityPanel {

  public ColumnConstraintPanel() {
    super(false, false);
  }

  /** {@inheritDoc} */
  public void initialize() {
    if (isInitialized())
      return;

    super.initialize();
    getTablePanel().setSearchPanelVisible(true);
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
