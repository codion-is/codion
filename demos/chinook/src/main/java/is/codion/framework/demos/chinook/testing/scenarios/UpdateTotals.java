/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.List;
import java.util.Random;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class UpdateTotals extends AbstractEntityUsageScenario<ChinookAppModel> {

  private final Random random = new Random();

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
    customerModel.tableModel().refresh();
    selectRandomRows(customerModel.tableModel(), random.nextInt(6) + 2);
    SwingEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    selectRandomRows(invoiceModel.tableModel(), random.nextInt(6) + 2);
    SwingEntityTableModel invoiceLineTableModel =
            invoiceModel.detailModel(InvoiceLine.TYPE).tableModel();
    List<Entity> invoiceLines = invoiceLineTableModel.items();
    Entity.put(InvoiceLine.QUANTITY, random.nextInt(4) + 1, invoiceLines);

    invoiceLineTableModel.update(invoiceLines);
  }
}
