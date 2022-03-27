/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class CustomerPanel extends EntityPanel {

  public CustomerPanel(SwingEntityModel customerModel) {
    super(customerModel, new CustomerEditPanel(customerModel.getEditModel()), new CustomerTablePanel(customerModel.getTableModel()));

    SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    SwingEntityModel invoiceLineModel = invoiceModel.getDetailModel(InvoiceLine.TYPE);

    InvoiceLineTablePanel invoiceLineTablePanel = new InvoiceLineTablePanel(invoiceLineModel.getTableModel());
    InvoiceLineEditPanel invoiceLineEditPanel = new InvoiceLineEditPanel(invoiceLineModel.getEditModel(),
            invoiceLineTablePanel.getTable().getSearchField());

    EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel, invoiceLineEditPanel, invoiceLineTablePanel);
    invoiceLinePanel.setIncludeControlPanel(false);

    addDetailPanel(new InvoicePanel(invoiceModel, invoiceLinePanel));
  }
}
