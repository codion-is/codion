/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class ChinookApplicationModel extends SwingEntityApplicationModel {

  public ChinookApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    addEntityModel(initializeArtistModel(connectionProvider));
    addEntityModel(initializePlaylistModel(connectionProvider));
    addEntityModel(initializeCustomerModel(connectionProvider));
  }

  private static SwingEntityModel initializeArtistModel(final EntityConnectionProvider connectionProvider) {
    final SwingEntityModel artistModel = new SwingEntityModel(Artist.TYPE, connectionProvider);
    final SwingEntityModel albumModel = new SwingEntityModel(new AlbumTableModel(connectionProvider));
    final SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));

    albumModel.addDetailModel(trackModel);
    artistModel.addDetailModel(albumModel);

    artistModel.getTableModel().refresh();

    return artistModel;
  }

  private static SwingEntityModel initializePlaylistModel(final EntityConnectionProvider connectionProvider) {
    final SwingEntityModel playlistModel = new SwingEntityModel(new PlaylistTableModel(connectionProvider));
    final SwingEntityModel playlistTrackModel = new SwingEntityModel(PlaylistTrack.TYPE, connectionProvider);

    playlistModel.addDetailModel(playlistTrackModel);

    playlistModel.getTableModel().refresh();

    return playlistModel;
  }

  private static SwingEntityModel initializeCustomerModel(final EntityConnectionProvider connectionProvider) {
    final SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
    final SwingEntityModel invoiceModel = new InvoiceModel(connectionProvider);
    customerModel.addDetailModel(invoiceModel);

    customerModel.getTableModel().refresh();

    return customerModel;
  }
}
