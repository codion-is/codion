/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

public final class InvoiceModel extends SwingEntityModel {

  public InvoiceModel(final EntityConnectionProvider connectionProvider) {
    super(new InvoiceEditModel(connectionProvider));

    final InvoiceLineEditModel invoiceLineEditModel = new InvoiceLineEditModel(connectionProvider);

    final SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
    invoiceLineModel.getEditModel().setInitializeForeignKeyToNull(true);

    addDetailModel(invoiceLineModel);
    addLinkedDetailModel(invoiceLineModel);

    invoiceLineEditModel.addTotalsUpdatedListener(getTableModel()::replaceEntities);
  }
}
