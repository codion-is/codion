/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.javafx;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.javafx.framework.model.FXEntityModel;
import is.codion.javafx.framework.ui.EntityApplicationView;
import is.codion.javafx.framework.ui.EntityView;

public final class ChinookFXAppView extends EntityApplicationView<ChinookFXAppModel> {

  public ChinookFXAppView() {
    super("Chinook FX Demo");
  }

  @Override
  protected void createEntityViews() {
    ChinookFXAppModel model = model();

    FXEntityModel artistModel = model.entityModel(Artist.TYPE);
    EntityView artists = new EntityView(artistModel, new ArtistEditVew(artistModel.editModel()));
    FXEntityModel albumModel = artistModel.detailModel(Album.TYPE);
    EntityView albums = new EntityView(albumModel);
    FXEntityModel trackModel = albumModel.detailModel(Track.TYPE);
    EntityView tracks = new EntityView(trackModel);

    artists.addDetailView(albums);
    albums.addDetailView(tracks);

    addEntityView(artists);

    //playlists
    FXEntityModel playlistModel = model.entityModel(Playlist.TYPE);
    EntityView playlists = new EntityView(playlistModel);
    FXEntityModel playlisttrackModel = playlistModel.detailModel(PlaylistTrack.TYPE);
    EntityView playlisttracks = new EntityView(playlisttrackModel);

    playlists.addDetailView(playlisttracks);

    addEntityView(playlists);

    //customers
    FXEntityModel customerModel = model.entityModel(Customer.TYPE);
    EntityView customers = new EntityView(customerModel);
    FXEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    EntityView invoices = new EntityView(invoiceModel);
    FXEntityModel invoicelineModel = invoiceModel.detailModel(InvoiceLine.TYPE);
    EntityView invoicelines = new EntityView(invoicelineModel);

    customers.addDetailView(invoices);
    invoices.addDetailView(invoicelines);

    addEntityView(customers);
  }

  @Override
  protected ChinookFXAppModel createApplicationModel(EntityConnectionProvider connectionProvider) {
    return new ChinookFXAppModel(connectionProvider);
  }

  @Override
  protected User defaultUser() {
    return User.parse("scott:tiger");
  }

  public static void main(String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_TYPE.set(Chinook.DOMAIN);
    launch(args);
  }
}
