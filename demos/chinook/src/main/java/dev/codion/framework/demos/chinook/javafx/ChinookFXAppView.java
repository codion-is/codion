/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.javafx;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.javafx.framework.model.FXEntityModel;
import dev.codion.javafx.framework.ui.EntityApplicationView;
import dev.codion.javafx.framework.ui.EntityView;

public class ChinookFXAppView extends EntityApplicationView<ChinookFXAppModel> {

  public ChinookFXAppView() {
    super("Chinook FX Demo");
  }

  @Override
  protected void initializeEntityViews() {
    final ChinookFXAppModel model = getModel();

    final FXEntityModel artistModel = model.getEntityModel(Chinook.T_ARTIST);
    final EntityView artists = new EntityView(artistModel, new ArtistEditVew(artistModel.getEditModel()));
    final FXEntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
    final EntityView albums = new EntityView(albumModel);
    final FXEntityModel trackModel = albumModel.getDetailModel(Chinook.T_TRACK);
    final EntityView tracks = new EntityView(trackModel);

    artists.addDetailView(albums);
    albums.addDetailView(tracks);

    addEntityView(artists);

    //playlists
    final FXEntityModel playlistModel = model.getEntityModel(Chinook.T_PLAYLIST);
    final EntityView playlists = new EntityView(playlistModel);
    final FXEntityModel playlisttrackModel = playlistModel.getDetailModel(Chinook.T_PLAYLISTTRACK);
    final EntityView playlisttracks = new EntityView(playlisttrackModel);

    playlists.addDetailView(playlisttracks);

    addEntityView(playlists);

    //customers
    final FXEntityModel customerModel = model.getEntityModel(Chinook.T_CUSTOMER);
    final EntityView customers = new EntityView(customerModel);
    final FXEntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    final EntityView invoices = new EntityView(invoiceModel);
    final FXEntityModel invoicelineModel = invoiceModel.getDetailModel(Chinook.T_INVOICELINE);
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
    return Users.parseUser("scott:tiger");
  }

  public static void main(final String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("dev.codion.framework.demos.chinook.domain.impl.ChinookImpl");
    launch(args);
  }
}
