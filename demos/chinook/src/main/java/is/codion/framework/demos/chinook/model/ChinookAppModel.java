/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class ChinookAppModel extends SwingEntityApplicationModel {

  public static final Version VERSION = Version.parsePropertiesFile(ChinookAppModel.class, "/version.properties");

  public ChinookAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider, VERSION);
    addEntityModel(createAlbumModel(connectionProvider));
    addEntityModel(createPlaylistModel(connectionProvider));
    addEntityModel(createCustomerModel(connectionProvider));
  }

  private static SwingEntityModel createAlbumModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connectionProvider);
    SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));
    trackModel.editModel().initializeComboBoxModels(Track.MEDIATYPE_FK, Track.GENRE_FK);

    albumModel.addDetailModel(trackModel);

    albumModel.tableModel().refresh();

    return albumModel;
  }

  private static SwingEntityModel createPlaylistModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel playlistModel = new SwingEntityModel(new PlaylistTableModel(connectionProvider));
    SwingEntityModel playlistTrackModel = new SwingEntityModel(PlaylistTrack.TYPE, connectionProvider);
    playlistTrackModel.editModel().initializeComboBoxModels(PlaylistTrack.PLAYLIST_FK);

    playlistModel.addDetailModel(playlistTrackModel);

    playlistModel.tableModel().refresh();

    return playlistModel;
  }

  private static SwingEntityModel createCustomerModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
    customerModel.editModel().initializeComboBoxModels(Customer.SUPPORTREP_FK);
    SwingEntityModel invoiceModel = new InvoiceModel(connectionProvider);
    customerModel.addDetailModel(invoiceModel);

    customerModel.tableModel().refresh();

    return customerModel;
  }
}
