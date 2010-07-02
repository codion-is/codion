/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;

public class ColumnConstraintPanel extends EntityPanel {

  public ColumnConstraintPanel(final EntityModel model) {
    super(model, "Column constraints");
    getTablePanel().setSearchPanelVisible(true);
  }
}
