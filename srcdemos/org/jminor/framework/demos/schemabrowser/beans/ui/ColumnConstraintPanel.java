/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;

import javax.swing.JPanel;

public class ColumnConstraintPanel extends EntityPanel {

  public ColumnConstraintPanel(final EntityModel model) {
    super(model, false, false);
  }

  /** {@inheritDoc} */
  @Override
  public void initialize() {
    if (isInitialized())
      return;

    super.initialize();
    getTablePanel().setSearchPanelVisible(true);
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
