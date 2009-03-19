/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.ColumnConstraintModel;

import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;

public class ConstraintPanel extends EntityPanel {

  public ConstraintPanel() {
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
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ColumnConstraintModel.class, ColumnConstraintPanel.class));
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
