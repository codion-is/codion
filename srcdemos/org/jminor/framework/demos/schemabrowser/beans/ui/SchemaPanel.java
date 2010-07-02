/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.schemabrowser.beans.DbObjectModel;

public class SchemaPanel extends EntityPanel {

  public SchemaPanel(final EntityModel model) {
    super(model, "Schema Users");
    setDetailSplitPanelResizeWeight(0.3);
    addDetailPanel(new DbObjectPanel(model.getDetailModel(DbObjectModel.class)));
    getTablePanel().setSearchPanelVisible(true);
  }
}
