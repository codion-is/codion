/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public final class ChinookApplicationModel extends SwingEntityApplicationModel {

  public ChinookApplicationModel(final EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    final SwingEntityModel artistModel = new SwingEntityModel(T_ARTIST, connectionProvider);
    final SwingEntityModel albumModel = new SwingEntityModel(T_ALBUM, connectionProvider);
    final SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));

    albumModel.addDetailModel(trackModel);
    artistModel.addDetailModel(albumModel);
    addEntityModel(artistModel);

    final SwingEntityModel playlistModel = new SwingEntityModel(T_PLAYLIST, connectionProvider);
    final SwingEntityModel playlistTrackModel = new SwingEntityModel(T_PLAYLISTTRACK, connectionProvider);

    playlistModel.addDetailModel(playlistTrackModel);
    addEntityModel(playlistModel);

    final SwingEntityModel customerModel = new SwingEntityModel(T_CUSTOMER, connectionProvider);
    final SwingEntityModel invoiceModel = new SwingEntityModel(new InvoiceEditModel(connectionProvider));
    final SwingEntityModel invoiceLineModel = new SwingEntityModel(T_INVOICELINE, connectionProvider);
    invoiceModel.addDetailModel(invoiceLineModel);
    invoiceModel.addLinkedDetailModel(invoiceLineModel);
    customerModel.addDetailModel(invoiceModel);
    addEntityModel(customerModel);

    artistModel.refresh();
    playlistModel.refresh();
    customerModel.refresh();
  }

  public void updateInvoiceTotals() throws DatabaseException {
    getConnectionProvider().getConnection().executeProcedure(P_UPDATE_TOTALS);
  }
}
