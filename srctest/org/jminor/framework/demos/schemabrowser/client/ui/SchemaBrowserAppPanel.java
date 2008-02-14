/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.apache.log4j.Level;
import org.jminor.common.db.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.beans.ui.SchemaPanel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;

public class SchemaBrowserAppPanel extends EntityApplicationPanel {

  /** {@inheritDoc} */
  protected List<EntityPanel.EntityPanelInfo> getRootEntityPanelInfo() {
    return Arrays.asList(new EntityPanel.EntityPanelInfo(SchemaModel.class, SchemaPanel.class));
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    Util.setDefaultLoggingLevel(Level.DEBUG);
    FrameworkSettings.get().tableAutoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS;
    FrameworkSettings.get().useSmartRefresh = false;
    FrameworkSettings.get().useQueryRange = false;
    FrameworkSettings.get().usernamePrefix = "";
    startApplication("Schema Browser", SchemaBrowserAppPanel.class, SchemaBrowserAppModel.class,
            null, false, UiUtil.getSize(0.5), new User("darri", "nonlocal"));
  }
}