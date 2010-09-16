/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;

public class InvoiceModel extends DefaultEntityModel {

  public InvoiceModel(final EntityDbProvider dbProvider) {
    super(Chinook.T_INVOICE, dbProvider);
    addInvoiceLineModel();
  }

  private void addInvoiceLineModel() {
    final EntityModel invoiceLineModel = new DefaultEntityModel(Chinook.T_INVOICELINE, getDbProvider());
    invoiceLineModel.getTableModel().setQueryConfigurationAllowed(false);
    addDetailModel(invoiceLineModel);
    setLinkedDetailModels(invoiceLineModel);
  }
}
