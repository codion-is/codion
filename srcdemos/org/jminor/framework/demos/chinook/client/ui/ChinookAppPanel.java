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
import org.jminor.framework.demos.chinook.beans.ArtistModel;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import org.jminor.framework.demos.chinook.beans.EmployeeModel;
import org.jminor.framework.demos.chinook.beans.GenreModel;
import org.jminor.framework.demos.chinook.beans.MediaTypeModel;
import org.jminor.framework.demos.chinook.beans.PlaylistModel;
import org.jminor.framework.demos.chinook.beans.ui.ArtistPanel;
import org.jminor.framework.demos.chinook.beans.ui.CustomerPanel;
import org.jminor.framework.demos.chinook.beans.ui.EmployeePanel;
import org.jminor.framework.demos.chinook.beans.ui.GenrePanel;
import org.jminor.framework.demos.chinook.beans.ui.MediaTypePanel;
import org.jminor.framework.demos.chinook.beans.ui.PlaylistPanel;
import org.jminor.framework.demos.chinook.client.ChinookAppModel;

import org.apache.log4j.Level;

import javax.swing.UIManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:17:25
 */
public class ChinookAppPanel extends EntityApplicationPanel {

  @Override
  protected List<EntityPanelProvider> getMainEntityPanelProviders() {
    return Arrays.asList(
            new EntityPanelProvider(ArtistModel.class, ArtistPanel.class),
            new EntityPanelProvider(PlaylistModel.class, PlaylistPanel.class),
            new EntityPanelProvider(CustomerModel.class, CustomerPanel.class));
  }

  @Override
  protected List<EntityPanelProvider> getSupportEntityPanelProviders() {
    return Arrays.asList(
            new EntityPanelProvider("Genres", GenreModel.class, GenrePanel.class),
            new EntityPanelProvider("Media types", MediaTypeModel.class, MediaTypePanel.class),
            new EntityPanelProvider("Employees", EmployeeModel.class, EmployeePanel.class));
  }

  @Override
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
    Locale.setDefault(new Locale("EN", "en"));
    new ChinookAppPanel().startApplication("Chinook", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }
}
