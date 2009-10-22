/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
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

  @Override
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(SchemaModel.class, SchemaPanel.class));
  }

  @Override
  protected void configureApplication() {
    Util.setDefaultLoggingLevel(Level.DEBUG);
    Configuration.setValue(Configuration.TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final User user) throws UserCancelException {
    return new SchemaBrowserAppModel(user);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    startApplication("Schema Browser", new SchemaBrowserAppPanel(), null, false, UiUtil.getScreenSizeRatio(0.5));
  }
}