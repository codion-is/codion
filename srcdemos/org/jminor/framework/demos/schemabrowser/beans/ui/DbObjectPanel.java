/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.ColumnModel;
import org.jminor.framework.demos.schemabrowser.beans.ConstraintModel;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

public class DbObjectPanel extends EntityPanel {

  public DbObjectPanel(final EntityModel model) {
    super(model, false, false, false, EMBEDDED);
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
    return Arrays.asList(
            new EntityPanelProvider(ColumnModel.class, ColumnPanel.class),
            new EntityPanelProvider(ConstraintModel.class, ConstraintPanel.class));
  }

  /** {@inheritDoc} */
  protected Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width/1.7), (int) (parentSize.height/1.7)-54);
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
