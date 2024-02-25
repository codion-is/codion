/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.model.loadtest.AbstractUsageScenario;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomTrackId;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

public final class InsertDeleteInvoice extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();

    Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
    Entity invoice = connection.insertSelect(connection.entities().builder(Invoice.TYPE)
            .with(Invoice.CUSTOMER_FK, customer)
            .with(Invoice.DATE, LocalDate.now())
            .with(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS))
            .with(Invoice.BILLINGCITY, customer.get(Customer.CITY))
            .with(Invoice.BILLINGPOSTALCODE, customer.get(Customer.POSTALCODE))
            .with(Invoice.BILLINGSTATE, customer.get(Customer.STATE))
            .with(Invoice.BILLINGCOUNTRY, customer.get(Customer.COUNTRY))
            .build());

    Set<Long> invoiceTrackIds = IntStream.range(0, 10)
            .mapToObj(i -> randomTrackId())
            .collect(toSet());
    List<Entity> invoiceLines = new ArrayList<>();
    for (Entity track : connection.select(Track.ID.in(invoiceTrackIds))) {
      connection.beginTransaction();
      try {
        invoiceLines.add(connection.insertSelect(connection.entities().builder(InvoiceLine.TYPE)
                .with(InvoiceLine.INVOICE_FK, invoice)
                .with(InvoiceLine.TRACK_FK, track)
                .with(InvoiceLine.QUANTITY, 1)
                .with(InvoiceLine.UNITPRICE, track.get(Track.UNITPRICE))
                .build()));
        connection.execute(Invoice.UPDATE_TOTALS, singletonList(invoice.get(Invoice.ID)));
        connection.commitTransaction();
      }
      catch (Exception e) {
        connection.rollbackTransaction();
        throw e;
      }
    }

    List<Entity> toDelete = new ArrayList<>(invoiceLines);
    toDelete.add(invoice);

    connection.delete(primaryKeys(toDelete));
  }

  @Override
  public int defaultWeight() {
    return 3;
  }
}
