/*
 * Copyright (c) 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static is.codion.framework.domain.entity.condition.Condition.key;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class InvoiceLineEditModelTest {

  @Test
  void updateTotals() throws DatabaseException, ValidationException {
    try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
      EntityConnection connection = connectionProvider.connection();

      Entity invoice = createInvoice(connectionProvider);
      assertNull(invoice.get(Invoice.TOTAL));

      Entity trackBattery = connection.selectSingle(Track.NAME.equalToIgnoreCase("battery"));

      InvoiceLineEditModel editModel = new InvoiceLineEditModel(connectionProvider);
      editModel.put(InvoiceLine.INVOICE_FK, invoice);
      editModel.put(InvoiceLine.TRACK_FK, trackBattery);
      Entity invoiceLineBattery = editModel.insert();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(BigDecimal.valueOf(0.99), invoice.get(Invoice.TOTAL));

      Entity trackOrion = connection.selectSingle(Track.NAME.equalToIgnoreCase("orion"));
      editModel.setDefaults();
      editModel.put(InvoiceLine.INVOICE_FK, invoice);
      editModel.put(InvoiceLine.TRACK_FK, trackOrion);
      editModel.insert();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(BigDecimal.valueOf(1.98), invoice.get(Invoice.TOTAL));

      editModel.set(invoiceLineBattery);
      editModel.delete();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(BigDecimal.valueOf(0.99), invoice.get(Invoice.TOTAL));
    }
  }

  private static Entity createInvoice(EntityConnectionProvider connectionProvider) throws DatabaseException {
    Entities entities = connectionProvider.entities();
    EntityConnection connection = connectionProvider.connection();

    return connection.insertSelect(entities.builder(Invoice.TYPE)
            .with(Invoice.CUSTOMER_FK, connection.insertSelect(entities.builder(Customer.TYPE)
                    .with(Customer.FIRSTNAME, "Björn")
                    .with(Customer.LASTNAME, "Sigurðsson")
                    .with(Customer.EMAIL, "email@email.com")
                    .build()))
            .with(Invoice.DATE, LocalDate.now())
            .build());
  }

  private static EntityConnectionProvider createConnectionProvider() {
    return LocalEntityConnectionProvider.builder()
            .domain(new ChinookImpl())
            .user(User.parse("scott:tiger"))
            .build();
  }
}
