/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.DbObjectModel;

import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;

public class SchemaPanel extends EntityPanel {

  public SchemaPanel(final EntityModel model) throws UserException {
    super(model, "Schema Users", true, false);
  }

  /** {@inheritDoc} */
  @Override
  protected void postInitialization() {
    getTablePanel().setSearchPanelVisible(true);
  }

  /** {@inheritDoc} */
  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(DbObjectModel.class, DbObjectPanel.class));
  }

  /** {@inheritDoc} */
  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.3;
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    return null;
  }
}
