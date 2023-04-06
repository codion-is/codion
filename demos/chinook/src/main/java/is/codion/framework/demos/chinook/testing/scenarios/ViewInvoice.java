/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

public final class ViewInvoice extends AbstractEntityUsageScenario<ChinookAppModel> {

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    SwingEntityModel customerModel = application.entityModel(Customer.TYPE);
    customerModel.tableModel().refresh();
    selectRandomRow(customerModel.tableModel());
    SwingEntityModel invoiceModel = customerModel.detailModel(Invoice.TYPE);
    selectRandomRow(invoiceModel.tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
