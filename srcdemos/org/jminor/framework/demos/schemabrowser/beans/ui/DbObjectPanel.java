/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.schemabrowser.beans.ColumnModel;
import org.jminor.framework.demos.schemabrowser.beans.ConstraintModel;

import java.awt.Dimension;

public class DbObjectPanel extends EntityPanel {

  public DbObjectPanel(final EntityModel model) {
    super(model, "Tables");
    setDetailPanelState(EMBEDDED);
    addDetailPanels(
            new ColumnPanel(model.getDetailModel(ColumnModel.class)),
            new ConstraintPanel(model.getDetailModel(ConstraintModel.class)));
    getTablePanel().setSearchPanelVisible(true);
  }

  @Override
  protected Dimension getDetailDialogSize(final Dimension parentSize) {
    return new Dimension((int) (parentSize.width/1.7), (int) (parentSize.height/1.7)-54);
  }
}
