/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

public final class CustomerPanel extends EntityPanel {

  public CustomerPanel(SwingEntityModel customerModel) {
    super(customerModel, new CustomerEditPanel(customerModel.editModel()), new CustomerTablePanel(customerModel.tableModel()));

    SwingEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    SwingEntityModel invoiceLineModel = invoiceModel.detailModel(InvoiceLine.TYPE);

    InvoiceLineTablePanel invoiceLineTablePanel = new InvoiceLineTablePanel(invoiceLineModel.tableModel());
    InvoiceLineEditPanel invoiceLineEditPanel = new InvoiceLineEditPanel(invoiceLineModel.editModel(),
            invoiceLineTablePanel.table().searchField());

    EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel, invoiceLineEditPanel, invoiceLineTablePanel);
    invoiceLinePanel.setIncludeControlPanel(false);

    addDetailPanel(new InvoicePanel(invoiceModel, invoiceLinePanel));
  }
}
