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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.testing.scenarios;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.tools.loadtest.Scenario.Performer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static is.codion.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;
import static is.codion.demos.chinook.testing.scenarios.LoadTestUtil.randomTrackId;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static java.util.stream.Collectors.toSet;

public final class InsertDeleteInvoice implements Performer<EntityConnectionProvider> {

	@Override
	public void perform(EntityConnectionProvider connectionProvider) {
		EntityConnection connection = connectionProvider.connection();

		Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
		Entity invoice = connection.insertSelect(connection.entities().entity(Invoice.TYPE)
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
			transaction(connection, () -> {
				invoiceLines.add(connection.insertSelect(connection.entities().entity(InvoiceLine.TYPE)
								.with(InvoiceLine.INVOICE_FK, invoice)
								.with(InvoiceLine.TRACK_FK, track)
								.with(InvoiceLine.QUANTITY, 1)
								.with(InvoiceLine.UNITPRICE, track.get(Track.UNITPRICE))
								.build()));
				connection.execute(Invoice.UPDATE_TOTALS, List.of(invoice.get(Invoice.ID)));
			});
		}

		List<Entity> toDelete = new ArrayList<>(invoiceLines);
		toDelete.add(invoice);

		connection.delete(primaryKeys(toDelete));
	}
}
