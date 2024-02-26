/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;

public final class ViewInvoice implements Performer<EntityConnectionProvider> {

  @Override
  public void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
    List<Long> invoiceIds = connection.select(Invoice.ID, Invoice.CUSTOMER_FK.equalTo(customer));
    if (!invoiceIds.isEmpty()) {
      Entity invoice = connection.selectSingle(Invoice.ID.equalTo(invoiceIds.get(RANDOM.nextInt(invoiceIds.size()))));
      connection.select(InvoiceLine.INVOICE_FK.equalTo(invoice));
    }
  }
}
