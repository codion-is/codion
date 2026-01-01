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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.reactive.observer.Observer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;
import java.util.Objects;

import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.distinct;
import static is.codion.framework.domain.entity.Entity.primaryKeys;

public final class InvoiceLineEditModel extends SwingEntityEditModel {

	public InvoiceLineEditModel(EntityConnectionProvider connectionProvider) {
		super(InvoiceLine.TYPE, connectionProvider);
		// We populate the unit price when the track is edited
		Observer<Entity> trackEdited = editor().value(InvoiceLine.TRACK_FK).edited();
		trackEdited.when(Objects::nonNull)
						.addConsumer(this::setUnitPrice);
		trackEdited.when(Objects::isNull)
						.addListener(this::clearUnitPrice);
	}

	@Override
	protected Collection<Entity> insert(Collection<Entity> invoiceLines, EntityConnection connection) {
		// Use a transaction to update the invoice totals when an invoice line is inserted
		return transaction(connection, () -> updateTotals(connection.insertSelect(invoiceLines), connection));
	}

	@Override
	protected Collection<Entity> update(Collection<Entity> invoiceLines, EntityConnection connection) {
		// Use a transaction to update the invoice totals when an invoice line is updated
		return transaction(connection, () -> updateTotals(connection.updateSelect(invoiceLines), connection));
	}

	@Override
	protected void delete(Collection<Entity> invoiceLines, EntityConnection connection) {
		// Use a transaction to update the invoice totals when an invoice line is deleted
		transaction(connection, () -> {
			connection.delete(primaryKeys(invoiceLines));
			updateTotals(invoiceLines, connection);
		});
	}

	private void setUnitPrice(Entity track) {
		editor().value(InvoiceLine.UNITPRICE).set(track.get(Track.UNITPRICE));
	}

	private void clearUnitPrice() {
		editor().value(InvoiceLine.UNITPRICE).clear();
	}

	// tag::updateTotals[]
	private static Collection<Entity> updateTotals(Collection<Entity> invoiceLines, EntityConnection connection) {
		// Get the IDs of the invoices that need their totals updated
		Collection<Long> invoiceIds = distinct(InvoiceLine.INVOICE_ID, invoiceLines);
		// Execute the UPDATE_TOTALS procedure
		connection.execute(Invoice.UPDATE_TOTALS, invoiceIds);

		return invoiceLines;
	}
	// end::updateTotals[]
}
