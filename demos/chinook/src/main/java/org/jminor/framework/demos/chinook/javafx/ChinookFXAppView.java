/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.javafx;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.javafx.framework.model.EntityApplicationModel;
import org.jminor.javafx.framework.model.EntityModel;
import org.jminor.javafx.framework.ui.EntityApplicationView;
import org.jminor.javafx.framework.ui.EntityView;

public class ChinookFXAppView extends EntityApplicationView<ChinookFXAppModel> {

  public ChinookFXAppView() {
    super("Chinook FX Demo");
  }

  @Override
  protected void initializeEntitieViews() {
    final EntityApplicationModel model = getModel();

    final EntityModel artistModel = model.getEntityModel(Chinook.T_ARTIST);
    final EntityView artists = new EntityView(artistModel);
    final EntityModel albumModel = artistModel.getDetailModel(Chinook.T_ALBUM);
    final EntityView albums = new EntityView(albumModel);
    final EntityModel trackModel = albumModel.getDetailModel(Chinook.T_TRACK);
    final EntityView tracks = new EntityView(trackModel);

    artists.addDetailView(albums);
    albums.addDetailView(tracks);

    addEntityView(artists);

    //playlists
    final EntityModel playlistModel = model.getEntityModel(Chinook.T_PLAYLIST);
    final EntityView playlists = new EntityView(playlistModel);
    final EntityModel playlisttrackModel = playlistModel.getDetailModel(Chinook.T_PLAYLISTTRACK);
    final EntityView playlisttracks = new EntityView(playlisttrackModel);

    playlists.addDetailView(playlisttracks);

    addEntityView(playlists);

    //customers
    final EntityModel customerModel = model.getEntityModel(Chinook.T_CUSTOMER);
    final EntityView customers = new EntityView(customerModel);
    final EntityModel invoiceModel = customerModel.getDetailModel(Chinook.T_INVOICE);
    final EntityView invoices = new EntityView(invoiceModel);
    final EntityModel invoicelineModel = invoiceModel.getDetailModel(Chinook.T_INVOICELINE);
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
    return new User("scott", "tiger");
  }
}
