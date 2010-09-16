/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class InvoiceModel extends DefaultEntityModel {

  public InvoiceModel(final EntityDbProvider dbProvider) {
    super(Chinook.T_INVOICE, dbProvider);
    addInvoiceLineModel();
  }

  private void addInvoiceLineModel() {
    final EntityModel invoiceLineModel = getDetailModel(Chinook.T_INVOICELINE);
    final EntityTableModel invoiceLineTableModel = invoiceLineModel.getTableModel();
    invoiceLineTableModel.setQueryConfigurationAllowed(false);
    invoiceLineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_UNITPRICE)
            .setSummaryType(PropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    invoiceLineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_TOTAL)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);
    invoiceLineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_QUANTITY)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);

    setLinkedDetailModels(invoiceLineModel);
    getEditModel().addValueMapSetListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> invoices = new ArrayList<Entity>();
        if (!getEditModel().isEntityNew()) {
          invoices.add(getEditModel().getEntityCopy());
        }
        getDetailModel(Chinook.T_INVOICELINE).initialize(Chinook.T_INVOICE, invoices);
      }
    });
  }
}
