/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.beans.ui.SchemaPanel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

import javax.swing.JTable;

public class SchemaBrowserAppPanel extends EntityApplicationPanel {

  public SchemaBrowserAppPanel() {
    addMainApplicationPanelProvider(new EntityPanelProvider(SchemaBrowser.T_SCHEMA, SchemaModel.class, SchemaPanel.class));
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new SchemaBrowserAppModel(dbProvider);
  }

  public static void main(final String[] args) {
    new SchemaBrowserAppPanel().startApplication("Schema Browser", null, false, UiUtil.getScreenSizeRatio(0.5));
  }
}