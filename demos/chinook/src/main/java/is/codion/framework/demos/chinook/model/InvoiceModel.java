/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityModel;

public final class InvoiceModel extends SwingEntityModel {

  public InvoiceModel(EntityConnectionProvider connectionProvider) {
    super(new InvoiceEditModel(connectionProvider));

    InvoiceLineEditModel invoiceLineEditModel = new InvoiceLineEditModel(connectionProvider);

    SwingEntityModel invoiceLineModel = new SwingEntityModel(invoiceLineEditModel);
    invoiceLineModel.editModel().setInitializeForeignKeyToNull(true);

    addDetailModel(invoiceLineModel);
    addLinkedDetailModel(invoiceLineModel);

    invoiceLineEditModel.addTotalsUpdatedListener(tableModel()::replaceEntities);
  }
}
