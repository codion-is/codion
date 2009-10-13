/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.DbObjectModel;

import java.util.Arrays;
import java.util.List;

public class SchemaPanel extends EntityPanel {

  public SchemaPanel(final EntityModel model) {
    super(model, "Schema Users", true, false);
  }

  @Override
  protected void initialize() {
    getTablePanel().setSearchPanelVisible(true);
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(DbObjectModel.class, DbObjectPanel.class));
  }

  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.3;
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return null;
  }
}
