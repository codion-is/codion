/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.beans.ui.SchemaPanel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;

import org.apache.log4j.Level;

import javax.swing.JTable;
import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;

public class SchemaBrowserAppPanel extends EntityApplicationPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(SchemaModel.class, SchemaPanel.class));
  }

  protected void initializeSettings() {
    Util.setDefaultLoggingLevel(Level.DEBUG);
    FrameworkSettings.get().setProperty(FrameworkSettings.TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    startApplication("Schema Browser", SchemaBrowserAppPanel.class, SchemaBrowserAppModel.class,
            null, false, UiUtil.getSize(0.5));
  }
}