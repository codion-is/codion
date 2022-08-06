/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class ChinookApplicationModel extends SwingEntityApplicationModel {

  public ChinookApplicationModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    addEntityModel(initializeArtistModel(connectionProvider));
    addEntityModel(initializePlaylistModel(connectionProvider));
    addEntityModel(initializeCustomerModel(connectionProvider));
  }

  private static SwingEntityModel initializeArtistModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel artistModel = new SwingEntityModel(Artist.TYPE, connectionProvider);
    SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connectionProvider);
    SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));
    trackModel.editModel().initializeComboBoxModels(Track.MEDIATYPE_FK, Track.GENRE_FK);

    albumModel.addDetailModel(trackModel);
    artistModel.addDetailModel(albumModel);

    artistModel.tableModel().refresh();

    return artistModel;
  }

  private static SwingEntityModel initializePlaylistModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel playlistModel = new SwingEntityModel(new PlaylistTableModel(connectionProvider));
    SwingEntityModel playlistTrackModel = new SwingEntityModel(PlaylistTrack.TYPE, connectionProvider);
    playlistTrackModel.editModel().initializeComboBoxModels(PlaylistTrack.PLAYLIST_FK);

    playlistModel.addDetailModel(playlistTrackModel);

    playlistModel.tableModel().refresh();

    return playlistModel;
  }

  private static SwingEntityModel initializeCustomerModel(EntityConnectionProvider connectionProvider) {
    SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
    customerModel.editModel().initializeComboBoxModels(Customer.SUPPORTREP_FK);
    SwingEntityModel invoiceModel = new InvoiceModel(connectionProvider);
    customerModel.addDetailModel(invoiceModel);

    customerModel.tableModel().refresh();

    return customerModel;
  }
}
