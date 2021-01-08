/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.javafx;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
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

public class ChinookFXAppView extends EntityApplicationView<ChinookFXAppModel> {

  public ChinookFXAppView() {
    super("Chinook FX Demo");
  }

  @Override
  protected void initializeEntityViews() {
    final ChinookFXAppModel model = getModel();

    final FXEntityModel artistModel = model.getEntityModel(Artist.TYPE);
    final EntityView artists = new EntityView(artistModel, new ArtistEditVew(artistModel.getEditModel()));
    final FXEntityModel albumModel = artistModel.getDetailModel(Album.TYPE);
    final EntityView albums = new EntityView(albumModel);
    final FXEntityModel trackModel = albumModel.getDetailModel(Track.TYPE);
    final EntityView tracks = new EntityView(trackModel);

    artists.addDetailView(albums);
    albums.addDetailView(tracks);

    addEntityView(artists);

    //playlists
    final FXEntityModel playlistModel = model.getEntityModel(Playlist.TYPE);
    final EntityView playlists = new EntityView(playlistModel);
    final FXEntityModel playlisttrackModel = playlistModel.getDetailModel(PlaylistTrack.TYPE);
    final EntityView playlisttracks = new EntityView(playlisttrackModel);

    playlists.addDetailView(playlisttracks);

    addEntityView(playlists);

    //customers
    final FXEntityModel customerModel = model.getEntityModel(Customer.TYPE);
    final EntityView customers = new EntityView(customerModel);
    final FXEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    final EntityView invoices = new EntityView(invoiceModel);
    final FXEntityModel invoicelineModel = invoiceModel.getDetailModel(InvoiceLine.TYPE);
    final EntityView invoicelines = new EntityView(invoicelineModel);

    customers.addDetailView(invoices);
    invoices.addDetailView(invoicelines);

    addEntityView(customers);
  }

  @Override
  protected ChinookFXAppModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
    return new ChinookFXAppModel(connectionProvider);
  }

  @Override
  protected User getDefaultUser() {
    return User.parseUser("scott:tiger");
  }

  public static void main(final String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.chinook.domain.impl.ChinookImpl");
    launch(args);
  }
}
