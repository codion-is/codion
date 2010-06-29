/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.beans.ui.SchemaPanel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;

import javax.swing.JTable;
import javax.swing.UIManager;

public class SchemaBrowserAppPanel extends EntityApplicationPanel {

  public SchemaBrowserAppPanel() {
    addMainApplicationPanelProvider(new EntityPanelProvider(SchemaModel.class, SchemaPanel.class));
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final User user) throws CancelException {
    return new SchemaBrowserAppModel(user);
  }

  public static void main(final String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    new SchemaBrowserAppPanel().startApplication("Schema Browser", null, false, UiUtil.getScreenSizeRatio(0.5));
  }
}