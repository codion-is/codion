/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.javafx;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.javafx.framework.model.FXEntityModel;

public class ChinookFXAppModel extends DefaultEntityApplicationModel {

  public ChinookFXAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  protected void setupEntityModels() {
    //artists
    final EntityModel artists = new FXEntityModel(Chinook.T_ARTIST, getConnectionProvider());
    final EntityModel albums = new FXEntityModel(Chinook.T_ALBUM, getConnectionProvider());
    final EntityModel tracks = new FXEntityModel(Chinook.T_TRACK, getConnectionProvider());

    artists.addDetailModel(albums);
    albums.addDetailModel(tracks);

    addEntityModel(artists);

    //playlists
    final FXEntityModel playlists = new FXEntityModel(Chinook.T_PLAYLIST, getConnectionProvider());
    final FXEntityModel playlisttracks = new FXEntityModel(Chinook.T_PLAYLISTTRACK, getConnectionProvider());

    playlists.addDetailModel(playlisttracks);

    addEntityModel(playlists);

    //customers
    final FXEntityModel customers = new FXEntityModel(Chinook.T_CUSTOMER, getConnectionProvider());
    final FXEntityModel invoices = new FXEntityModel(Chinook.T_INVOICE, getConnectionProvider());
    final FXEntityModel invoicelines = new FXEntityModel(Chinook.T_INVOICELINE, getConnectionProvider());

    customers.addDetailModel(invoices);
    invoices.addDetailModel(invoicelines);

    addEntityModel(customers);
  }

  @Override
  protected void loadDomainModel() {
    Chinook.init();
  }
}
