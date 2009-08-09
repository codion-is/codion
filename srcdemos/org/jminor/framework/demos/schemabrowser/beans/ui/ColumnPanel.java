/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;

import javax.swing.JPanel;

public class ColumnPanel extends EntityPanel {

  public ColumnPanel(final EntityModel model) {
    super(model, "Columns", false, false);
  }

  /** {@inheritDoc} */
  @Override
  protected void postInitialization() {
    getTablePanel().setSearchPanelVisible(true);
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
