/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
    invoiceLinePanel.setIncludeEditControls(false);

    addDetailPanel(new InvoicePanel(invoiceModel, invoiceLinePanel));
  }
}
