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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.model.SwingEntityModel;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class InvoiceModelTest {

	@Test
	void detailLinkFollowsAndClearsWithTheSelection() {
		try (EntityConnection connection = createConnection()) {
			InvoiceModel invoiceModel = new InvoiceModel(connection);
			SwingEntityModel invoiceLineModel = invoiceModel.detail().get(InvoiceLine.TYPE);

			Entity invoice = createInvoice(connection);
			invoiceModel.tableModel().items().refresh();
			invoiceModel.tableModel().selection().item().set(invoice);

			// The link is active from the start here (invoiceLineLinkActive), the InvoiceLine panel being embedded
			// in the edit panel rather than navigated to - so selecting an invoice reaches the detail editor
			// without the UI having to switch anything on.
			assertEquals(invoice, invoiceLineModel.editModel().editor().value(InvoiceLine.INVOICE_FK).get());

			// clearValueOnEmptySelection: without it the foreign key would keep pointing at the invoice just
			// deselected, and the next line inserted would silently land on it.
			invoiceModel.tableModel().selection().clear();
			assertNull(invoiceLineModel.editModel().editor().value(InvoiceLine.INVOICE_FK).get());
		}
	}

	@Test
	void aPersistedLineRefreshesItsInvoiceTotal() throws EntityValidationException {
		try (EntityConnection connection = createConnection()) {
			InvoiceModel invoiceModel = new InvoiceModel(connection);
			SwingEntityModel invoiceLineModel = invoiceModel.detail().get(InvoiceLine.TYPE);

			Entity invoice = createInvoice(connection);
			invoiceModel.tableModel().items().refresh();
			invoiceModel.tableModel().selection().item().set(invoice);
			assertEquals(BigDecimal.ZERO, invoiceModel.tableModel().selection().item().getOrThrow().get(Invoice.TOTAL));

			Entity battery = connection.selectSingle(Track.NAME.equalToIgnoreCase("battery"));
			SwingEntityEditor invoiceLineEditor = invoiceLineModel.editModel().editor();
			// defaults() first, for QUANTITY - the link supplies INVOICE_FK and nothing else, and a foreign key
			// set by a detail link persists across defaults(), which is the behaviour the first test pins.
			invoiceLineEditor.entity().defaults();
			assertEquals(invoice, invoiceLineEditor.value(InvoiceLine.INVOICE_FK).get());
			invoiceLineEditor.value(InvoiceLine.TRACK_FK).set(battery);
			invoiceLineEditor.insert();

			// The total is computed by the database, so the row in the table model is stale until something
			// refreshes it - which is what the persisted-events listener in InvoiceConfig is for.
			Entity refreshed = invoiceModel.tableModel().items().included().get().stream()
							.filter(row -> row.primaryKey().equals(invoice.primaryKey()))
							.findFirst()
							.orElseThrow();
			assertEquals(battery.get(Track.UNITPRICE), refreshed.get(Invoice.TOTAL));
		}
	}

	@Test
	void theDetailTableRequiresACondition() {
		try (EntityConnection connection = createConnection()) {
			InvoiceModel invoiceModel = new InvoiceModel(connection);
			SwingEntityModel invoiceLineModel = invoiceModel.detail().get(InvoiceLine.TYPE);

			// Adding a detail link sets this, and it is what keeps an unselected invoice from pulling every line
			// in the database into the detail table.
			assertTrue(invoiceLineModel.tableModel().query().conditionRequired().is());

			invoiceModel.tableModel().selection().clear();
			invoiceLineModel.tableModel().items().refresh();
			assertEquals(List.of(), invoiceLineModel.tableModel().items().included().get());
		}
	}

	private static Entity createInvoice(EntityConnection connection) {
		Entities entities = connection.entities();

		return connection.insertSelect(entities.entity(Invoice.TYPE)
						.with(Invoice.CUSTOMER_FK, connection.insertSelect(entities.entity(Customer.TYPE)
										.with(Customer.FIRSTNAME, "Jack")
										.with(Customer.LASTNAME, "Random")
										.with(Customer.EMAIL, "email@email.com")
										.build()))
						.with(Invoice.DATE, LocalDate.now())
						.build());
	}

	private static EntityConnection createConnection() {
		return LocalEntityConnection.builder()
						.domain(new ChinookImpl())
						.user(User.parse("scott:tiger"))
						.build();
	}
}
