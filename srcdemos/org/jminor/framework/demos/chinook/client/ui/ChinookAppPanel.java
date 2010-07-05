/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.ui.EntityApplicationPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.beans.ui.ArtistPanel;
import org.jminor.framework.demos.chinook.beans.ui.CustomerPanel;
import org.jminor.framework.demos.chinook.beans.ui.EmployeePanel;
import org.jminor.framework.demos.chinook.beans.ui.GenrePanel;
import org.jminor.framework.demos.chinook.beans.ui.MediaTypePanel;
import org.jminor.framework.demos.chinook.beans.ui.PlaylistPanel;
import org.jminor.framework.demos.chinook.client.ChinookAppModel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import java.util.Locale;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:17:25
 */
public class ChinookAppPanel extends EntityApplicationPanel {

  public ChinookAppPanel() {
    new EntityPanelProvider(Chinook.T_ARTIST).setEditPanelClass(ArtistPanel.class)
            .addDetailEntityID(Chinook.T_ALBUM).register();
    new EntityPanelProvider(Chinook.T_PLAYLIST).setPanelClass(PlaylistPanel.class)
            .addDetailEntityID(Chinook.T_PLAYLISTTRACK).register();
    new EntityPanelProvider(Chinook.T_CUSTOMER).setPanelClass(CustomerPanel.class)
            .addDetailEntityID(Chinook.T_INVOICE).register();

    new EntityPanelProvider(Chinook.T_GENRE, "Genres").setPanelClass(GenrePanel.class)
            .addDetailEntityID(Chinook.T_TRACK).register();
    new EntityPanelProvider(Chinook.T_MEDIATYPE, "Media types").setPanelClass(MediaTypePanel.class)
            .addDetailEntityID(Chinook.T_TRACK).register();
    new EntityPanelProvider(Chinook.T_EMPLOYEE, "Employees").setPanelClass(EmployeePanel.class)
            .addDetailEntityID(Chinook.T_CUSTOMER).register();
    
    addMainApplicationPanelProviders(
            new EntityPanelProvider(Chinook.T_ARTIST).setEditPanelClass(ArtistPanel.class)
                    .addDetailEntityID(Chinook.T_ALBUM),
            new EntityPanelProvider(Chinook.T_PLAYLIST).setPanelClass(PlaylistPanel.class)
                    .addDetailEntityID(Chinook.T_PLAYLISTTRACK),
            new EntityPanelProvider(Chinook.T_CUSTOMER).setPanelClass(CustomerPanel.class)
                    .addDetailEntityID(Chinook.T_INVOICE));
    addSupportPanelProviders(
            new EntityPanelProvider(Chinook.T_GENRE, "Genres").setPanelClass(GenrePanel.class),
            new EntityPanelProvider(Chinook.T_MEDIATYPE, "Media types").setPanelClass(MediaTypePanel.class),
            new EntityPanelProvider(Chinook.T_EMPLOYEE, "Employees").setPanelClass(EmployeePanel.class));
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new ChinookAppModel(dbProvider);
  }

  @Override
  protected void configureApplication() {
    Configuration.setValue(Configuration.TOOLBAR_BUTTONS, true);
    Configuration.setValue(Configuration.COMPACT_ENTITY_PANEL_LAYOUT, true);
    Configuration.setValue(Configuration.USE_OPTIMISTIC_LOCKING, true);
    Configuration.setValue(Configuration.PROPERTY_DEBUG_OUTPUT, true);
    Configuration.setValue(Configuration.AUTO_CREATE_ENTITY_MODELS, true);
  }

  public static void main(final String[] args) {
    Locale.setDefault(new Locale("EN", "en"));
    new ChinookAppPanel().startApplication("Chinook", null, false, UiUtil.getScreenSizeRatio(0.6), new User("scott", "tiger"));
  }
}
