/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.javafx;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.javafx.framework.model.FXEntityEditModel;
import org.jminor.javafx.framework.model.FXEntityModel;
import org.jminor.javafx.framework.ui.EntityApplicationView;
import org.jminor.javafx.framework.ui.EntityView;

public class ChinookFXAppView extends EntityApplicationView<ChinookFXAppModel> {

  public ChinookFXAppView() {
    super("Chinook FX Demo");
  }

  @Override
  protected void initializeEntityViews() {
    final ChinookFXAppModel model = getModel();

    final FXEntityModel artistModel = model.getEntityModel(Chinook.T_ARTIST);
    final EntityView artists = new EntityView(artistModel, new ArtistEditVew((FXEntityEditModel) artistModel.getEditModel()));
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
    return new User("scott", "tiger".toCharArray());
  }

  public static void main(final String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.chinook.domain.ChinookDomain");
    launch(args);
  }
}
