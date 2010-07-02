/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.schemabrowser.beans.ColumnConstraintModel;

public class ConstraintPanel extends EntityPanel {

  public ConstraintPanel(final EntityModel model) {
    super(model, "Constraints");
    addDetailPanel(new ColumnConstraintPanel(model.getDetailModel(ColumnConstraintModel.class)));
    getTablePanel().setSearchPanelVisible(true);
  }
}
