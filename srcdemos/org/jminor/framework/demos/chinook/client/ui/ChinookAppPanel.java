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
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.beans.ui.*;
import org.jminor.framework.demos.chinook.domain.Chinook;

import java.util.Locale;

public class ChinookAppPanel extends EntityApplicationPanel {

  public ChinookAppPanel() {
    final EntityPanelProvider trackProvider = new EntityPanelProvider(Chinook.T_TRACK)
            .setEditPanelClass(TrackPanel.class).register();
    final EntityPanelProvider albumProvider = new EntityPanelProvider(Chinook.T_ALBUM)
            .setEditPanelClass(AlbumPanel.class).register();
    albumProvider.addDetailPanelProvider(trackProvider);
    final EntityPanelProvider  artistProvider = new EntityPanelProvider(Chinook.T_ARTIST)
            .setEditPanelClass(ArtistPanel.class).register();
    artistProvider.addDetailPanelProvider(albumProvider).setDetailSplitPanelResizeWeight(0.3);

    final EntityPanelProvider playlistTrackProvider = new EntityPanelProvider(Chinook.T_PLAYLISTTRACK)
            .setEditPanelClass(PlaylistTrackPanel.class).register();
    final EntityPanelProvider playlistProvider = new EntityPanelProvider(Chinook.T_PLAYLIST)
            .setEditPanelClass(PlaylistPanel.class).register();
    playlistProvider.addDetailPanelProvider(playlistTrackProvider).setDetailSplitPanelResizeWeight(0.3);

    final EntityPanelProvider customerProvider = new EntityPanelProvider(Chinook.T_CUSTOMER)
            .setEditPanelClass(CustomerPanel.class).register();
    final EntityPanelProvider invoiceProvider = new EntityPanelProvider(Chinook.T_INVOICE)
            .setEditPanelClass(InvoicePanel.class).register();
    customerProvider.addDetailPanelProvider(invoiceProvider);
    final EntityPanelProvider invoiceLineProvider = new EntityPanelProvider(Chinook.T_INVOICELINE)
            .setEditPanelClass(InvoiceLinePanel.class).register();
    invoiceProvider.addDetailPanelProvider(invoiceLineProvider);

    final EntityPanelProvider genreProvider = new EntityPanelProvider(Chinook.T_GENRE, "Genres")
            .setEditPanelClass(GenrePanel.class).register();
    genreProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.HIDDEN);
    final EntityPanelProvider mediaTypeProvider = new EntityPanelProvider(Chinook.T_MEDIATYPE, "Media types")
            .setEditPanelClass(MediaTypePanel.class).register();
    mediaTypeProvider.addDetailPanelProvider(trackProvider).setDetailPanelState(EntityPanel.HIDDEN);
    final EntityPanelProvider employeeProvider = new EntityPanelProvider(Chinook.T_EMPLOYEE, "Employees")
            .setEditPanelClass(EmployeePanel.class).register();
    employeeProvider.addDetailPanelProvider(customerProvider).setDetailPanelState(EntityPanel.HIDDEN);

    addMainApplicationPanelProviders(artistProvider, playlistProvider, customerProvider);
    addSupportPanelProviders(genreProvider, mediaTypeProvider, employeeProvider);
  }

  @Override
  protected EntityApplicationModel initializeApplicationModel(final EntityDbProvider dbProvider) throws CancelException {
    return new EntityApplicationModel(dbProvider) {
      @Override
      protected void loadDomainModel() {
        new Chinook();
      }
    };
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
