/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.javafx;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.javafx.framework.model.FXEntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityModel;

public final class ChinookFXAppModel extends FXEntityApplicationModel {

  public ChinookFXAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  private void setupEntityModels() {
    //artists
    FXEntityModel artists = new FXEntityModel(Artist.TYPE, getConnectionProvider());
    FXEntityModel albums = new FXEntityModel(Album.TYPE, getConnectionProvider());
    FXEntityModel tracks = new FXEntityModel(Track.TYPE, getConnectionProvider());

    artists.addDetailModel(albums);
    albums.addDetailModel(tracks);

    addEntityModel(artists);

    //playlists
    FXEntityModel playlists = new FXEntityModel(Playlist.TYPE, getConnectionProvider());
    FXEntityModel playlisttracks = new FXEntityModel(PlaylistTrack.TYPE, getConnectionProvider());

    playlists.addDetailModel(playlisttracks);

    addEntityModel(playlists);

    //customers
    FXEntityModel customers = new FXEntityModel(Customer.TYPE, getConnectionProvider());
    FXEntityModel invoices = new FXEntityModel(Invoice.TYPE, getConnectionProvider());
    FXEntityModel invoicelines = new FXEntityModel(InvoiceLine.TYPE, getConnectionProvider());

    customers.addDetailModel(invoices);
    invoices.addDetailModel(invoicelines);

    addEntityModel(customers);
  }
}
