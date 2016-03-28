/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.javafx;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.javafx.framework.model.EntityApplicationModel;
import org.jminor.javafx.framework.model.EntityModel;

public class ChinookFXAppModel extends EntityApplicationModel {

  public ChinookFXAppModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    setupEntityModels();
  }

  protected void setupEntityModels() {
    //artists
    final EntityModel artists = new EntityModel(Chinook.T_ARTIST, getConnectionProvider());
    final EntityModel albums = new EntityModel(Chinook.T_ALBUM, getConnectionProvider());
    final EntityModel tracks = new EntityModel(Chinook.T_TRACK, getConnectionProvider());

    artists.addDetailModel(albums);
    albums.addDetailModel(tracks);

    addEntityModel(artists);

    //playlists
    final EntityModel playlists = new EntityModel(Chinook.T_PLAYLIST, getConnectionProvider());
    final EntityModel playlisttracks = new EntityModel(Chinook.T_PLAYLISTTRACK, getConnectionProvider());

    playlists.addDetailModel(playlisttracks);

    addEntityModel(playlists);

    //customers
    final EntityModel customers = new EntityModel(Chinook.T_CUSTOMER, getConnectionProvider());
    final EntityModel invoices = new EntityModel(Chinook.T_INVOICE, getConnectionProvider());
    final EntityModel invoicelines = new EntityModel(Chinook.T_INVOICELINE, getConnectionProvider());

    customers.addDetailModel(invoices);
    invoices.addDetailModel(invoicelines);

    addEntityModel(customers);
  }

  @Override
  protected void loadDomainModel() {
    Chinook.init();
  }
}
