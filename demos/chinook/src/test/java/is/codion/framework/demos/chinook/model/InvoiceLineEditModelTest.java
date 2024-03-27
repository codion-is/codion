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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
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

      Entity battery = connection.selectSingle(Track.NAME.equalToIgnoreCase("battery"));

      InvoiceLineEditModel editModel = new InvoiceLineEditModel(connectionProvider);
      editModel.put(InvoiceLine.INVOICE_FK, invoice);
      editModel.put(InvoiceLine.TRACK_FK, battery);
      Entity invoiceLineBattery = editModel.insert();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(battery.get(Track.UNITPRICE), invoice.get(Invoice.TOTAL));

      Entity orion = connection.selectSingle(Track.NAME.equalToIgnoreCase("orion"));
      editModel.defaults();
      editModel.put(InvoiceLine.INVOICE_FK, invoice);
      editModel.put(InvoiceLine.TRACK_FK, orion);
      editModel.insert();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(battery.get(Track.UNITPRICE).add(orion.get(Track.UNITPRICE)), invoice.get(Invoice.TOTAL));

      Entity theCallOfKtulu = connection.selectSingle(Track.NAME.equalToIgnoreCase("the call of ktulu"));
      theCallOfKtulu.put(Track.UNITPRICE, BigDecimal.valueOf(2));
      theCallOfKtulu = connection.updateSelect(theCallOfKtulu);

      editModel.set(invoiceLineBattery);
      editModel.put(InvoiceLine.TRACK_FK, theCallOfKtulu);
      editModel.update();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(orion.get(Track.UNITPRICE).add(theCallOfKtulu.get(Track.UNITPRICE)), invoice.get(Invoice.TOTAL));

      editModel.delete();

      invoice = connection.selectSingle(key(invoice.primaryKey()));
      assertEquals(orion.get(Track.UNITPRICE), invoice.get(Invoice.TOTAL));
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
