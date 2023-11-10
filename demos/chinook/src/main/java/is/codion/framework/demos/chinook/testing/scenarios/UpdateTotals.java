/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;

public final class UpdateTotals extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
    List<Long> invoiceIds = connection.select(Invoice.ID, Invoice.CUSTOMER_FK.equalTo(customer));
    if (!invoiceIds.isEmpty()) {
      Entity invoice = connection.selectSingle(Invoice.ID.equalTo(invoiceIds.get(RANDOM.nextInt(invoiceIds.size()))));
      Collection<Entity> invoiceLines = connection.select(InvoiceLine.INVOICE_FK.equalTo(invoice));
      invoiceLines.forEach(invoiceLine ->
              invoiceLine.put(InvoiceLine.QUANTITY, RANDOM.nextInt(4) + 1));
      updateInvoiceLines(invoiceLines.stream()
              .filter(Entity::modified)
              .collect(Collectors.toList()), connection);
    }
  }

  private static void updateInvoiceLines(Collection<Entity> invoiceLines, EntityConnection connection) throws DatabaseException {
    connection.beginTransaction();
    try {
      Collection<Entity> updated = connection.updateSelect(invoiceLines);
      connection.execute(Invoice.UPDATE_TOTALS, Entity.distinct(InvoiceLine.INVOICE_ID, updated));
      connection.commitTransaction();
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }
}
