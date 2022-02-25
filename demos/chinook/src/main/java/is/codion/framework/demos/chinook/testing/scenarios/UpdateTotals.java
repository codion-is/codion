/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.List;
import java.util.Random;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRows;

public final class UpdateTotals extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    SwingEntityModel customerModel = application.getEntityModel(Customer.TYPE);
    customerModel.getTableModel().refresh();
    selectRandomRows(customerModel.getTableModel(), random.nextInt(6) + 2);
    SwingEntityModel invoiceModel = customerModel.getDetailModel(Invoice.TYPE);
    selectRandomRows(invoiceModel.getTableModel(), random.nextInt(6) + 2);
    SwingEntityTableModel invoiceLineTableModel =
            invoiceModel.getDetailModel(InvoiceLine.TYPE).getTableModel();
    List<Entity> invoiceLines = invoiceLineTableModel.getItems();
    Entity.put(InvoiceLine.QUANTITY, random.nextInt(4) + 1, invoiceLines);

    invoiceLineTableModel.update(invoiceLines);
  }
}
