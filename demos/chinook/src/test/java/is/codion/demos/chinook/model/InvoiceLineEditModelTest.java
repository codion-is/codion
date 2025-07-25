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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel.EntityEditor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static is.codion.framework.domain.entity.condition.Condition.key;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class InvoiceLineEditModelTest {

	@Test
	void updateTotals() {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			EntityConnection connection = connectionProvider.connection();

			Entity invoice = createInvoice(connection);
			assertNull(invoice.get(Invoice.TOTAL));

			Entity battery = connection.selectSingle(Track.NAME.equalToIgnoreCase("battery"));

			InvoiceLineEditModel editModel = new InvoiceLineEditModel(connectionProvider);
			EntityEditor editor = editModel.editor();
			editor.value(InvoiceLine.INVOICE_FK).set(invoice);
			editor.value(InvoiceLine.TRACK_FK).set(battery);
			Entity invoiceLineBattery = editModel.insert();

			invoice = connection.selectSingle(key(invoice.primaryKey()));
			assertEquals(battery.get(Track.UNITPRICE), invoice.get(Invoice.TOTAL));

			Entity orion = connection.selectSingle(Track.NAME.equalToIgnoreCase("orion"));
			editor.defaults();
			editor.value(InvoiceLine.INVOICE_FK).set(invoice);
			editor.value(InvoiceLine.TRACK_FK).set(orion);
			editModel.insert();

			invoice = connection.selectSingle(key(invoice.primaryKey()));
			assertEquals(battery.get(Track.UNITPRICE).add(orion.get(Track.UNITPRICE)), invoice.get(Invoice.TOTAL));

			Entity theCallOfKtulu = connection.selectSingle(Track.NAME.equalToIgnoreCase("the call of ktulu"));
			theCallOfKtulu.set(Track.UNITPRICE, BigDecimal.valueOf(2));
			theCallOfKtulu = connection.updateSelect(theCallOfKtulu);

			editor.set(invoiceLineBattery);
			editor.value(InvoiceLine.TRACK_FK).set(theCallOfKtulu);
			editModel.update();

			invoice = connection.selectSingle(key(invoice.primaryKey()));
			assertEquals(orion.get(Track.UNITPRICE).add(theCallOfKtulu.get(Track.UNITPRICE)), invoice.get(Invoice.TOTAL));

			editModel.delete();

			invoice = connection.selectSingle(key(invoice.primaryKey()));
			assertEquals(orion.get(Track.UNITPRICE), invoice.get(Invoice.TOTAL));
		}
	}

	private static Entity createInvoice(EntityConnection connection) {
		Entities entities = connection.entities();

		return connection.insertSelect(entities.entity(Invoice.TYPE)
						.with(Invoice.CUSTOMER_FK, connection.insertSelect(entities.entity(Customer.TYPE)
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
