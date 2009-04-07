/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.DbObjectModel;

import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;

public class SchemaPanel extends EntityPanel {

  public SchemaPanel() throws UserException {
    super(true, false);
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
    return Arrays.asList(new EntityPanelProvider(DbObjectModel.class, DbObjectPanel.class));
  }

  /** {@inheritDoc} */
  protected double getDetailSplitPaneResizeWeight() {
    return 0.3;
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
