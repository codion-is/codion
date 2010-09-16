/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans;

import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Property;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class InvoiceModel extends DefaultEntityModel {

  public InvoiceModel(final EntityDbProvider dbProvider) {
    super(T_INVOICE, dbProvider);
    addInvoiceLineModel();
  }

  private void addInvoiceLineModel() {
    final EntityEditModel invoiceLineEditModel = new DefaultEntityEditModel(Chinook.T_INVOICELINE, getDbProvider()) {
      @Override
      public boolean persistValueOnClear(final Property property) {
        return property.is(INVOICELINE_INVOICEID_FK);
      }
    };
    final EntityModel invoiceLineModel = new DefaultEntityModel(invoiceLineEditModel);
    invoiceLineModel.getTableModel().setQueryConfigurationAllowed(false);
    addDetailModel(invoiceLineModel);
    setLinkedDetailModels(invoiceLineModel);
  }
}
