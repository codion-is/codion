/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.AlbumModel;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import org.jminor.framework.demos.chinook.beans.EmployeeModel;
import org.jminor.framework.demos.chinook.beans.GenreModel;
import org.jminor.framework.demos.chinook.beans.ui.AlbumPanel;
import org.jminor.framework.demos.chinook.beans.ui.CustomerPanel;
import org.jminor.framework.demos.chinook.beans.ui.EmployeePanel;
import org.jminor.framework.demos.chinook.beans.ui.GenrePanel;
import org.jminor.framework.demos.chinook.client.ChinookAppModel;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:17:25
 */
public class ChinookAppPanel extends EntityApplicationPanel {

  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(GenreModel.class, GenrePanel.class),
            new EntityPanelProvider(CustomerModel.class, CustomerPanel.class),
            new EntityPanelProvider(AlbumModel.class, AlbumPanel.class),
            new EntityPanelProvider(EmployeeModel.class, EmployeePanel.class));
  }

  protected EntityApplicationModel initializeApplicationModel(final User user) throws CancelException {
    return new ChinookAppModel(user);
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    Util.setLoggingLevel(Level.DEBUG);
  }

  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    new ChinookAppPanel().startApplication("Chinook", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }
}
