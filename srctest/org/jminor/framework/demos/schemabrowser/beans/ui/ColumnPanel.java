/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.ui.EntityPanel;

import javax.swing.JPanel;

public class ColumnPanel extends EntityPanel {

  public ColumnPanel() {
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
