/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;

public class ColumnPanel extends EntityPanel {

  public ColumnPanel(final EntityModel model) {
    super(model, "Columns", false, false);
  }

  @Override
  protected void initialize() {
    getTablePanel().setSearchPanelVisible(true);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return null;
  }
}
