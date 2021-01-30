/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.List;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class ChinookApplicationModel extends SwingEntityApplicationModel {

  public ChinookApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    final SwingEntityModel artistModel = new SwingEntityModel(Artist.TYPE, connectionProvider);
    final SwingEntityModel albumModel = new SwingEntityModel(Album.TYPE, connectionProvider);
    final SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));

    albumModel.addDetailModel(trackModel);
    artistModel.addDetailModel(albumModel);
    addEntityModel(artistModel);

    final SwingEntityModel playlistModel = new SwingEntityModel(Playlist.TYPE, connectionProvider);
    final SwingEntityModel playlistTrackModel = new SwingEntityModel(PlaylistTrack.TYPE, connectionProvider);

    playlistModel.addDetailModel(playlistTrackModel);
    addEntityModel(playlistModel);

    final SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
    final SwingEntityModel invoiceModel = new SwingEntityModel(new InvoiceEditModel(connectionProvider));
    final SwingEntityModel invoiceLineModel = new SwingEntityModel(InvoiceLine.TYPE, connectionProvider);
    invoiceModel.addDetailModel(invoiceLineModel);
    invoiceModel.addLinkedDetailModel(invoiceLineModel);
    customerModel.addDetailModel(invoiceModel);
    addEntityModel(customerModel);

    artistModel.refresh();
    playlistModel.refresh();
    customerModel.refresh();
  }

  public List<Entity> updateInvoiceTotals() throws DatabaseException {
    return getConnectionProvider().getConnection().executeFunction(Invoice.UPDATE_TOTALS);
  }
}
