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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;
import java.util.function.Consumer;

public final class InvoiceLineEditModel extends SwingEntityEditModel {

	private final Event<Collection<Entity>> totalsUpdatedEvent = Event.event();

	public InvoiceLineEditModel(EntityConnectionProvider connectionProvider) {
		super(InvoiceLine.TYPE, connectionProvider);
		value(InvoiceLine.TRACK_FK).edited().addConsumer(this::setUnitPrice);
	}

	void addTotalsUpdatedConsumer(Consumer<Collection<Entity>> consumer) {
		totalsUpdatedEvent.addConsumer(consumer);
	}

	@Override
	protected Collection<Entity> insert(Collection<Entity> invoiceLines, EntityConnection connection) throws DatabaseException {
		connection.startTransaction();
		try {
			Collection<Entity> inserted = connection.insertSelect(invoiceLines);
			updateTotals(inserted, connection);
			connection.commitTransaction();

			return inserted;
		}
		catch (DatabaseException e) {
			connection.rollbackTransaction();
			throw e;
		}
		catch (Exception e) {
			connection.rollbackTransaction();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Collection<Entity> update(Collection<Entity> invoiceLines, EntityConnection connection) throws DatabaseException {
		connection.startTransaction();
		try {
			Collection<Entity> updated = connection.updateSelect(invoiceLines);
			updateTotals(updated, connection);
			connection.commitTransaction();

			return updated;
		}
		catch (DatabaseException e) {
			connection.rollbackTransaction();
			throw e;
		}
		catch (Exception e) {
			connection.rollbackTransaction();
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void delete(Collection<Entity> invoiceLines, EntityConnection connection) throws DatabaseException {
		connection.startTransaction();
		try {
			connection.delete(Entity.primaryKeys(invoiceLines));
			updateTotals(invoiceLines, connection);
			connection.commitTransaction();
		}
		catch (DatabaseException e) {
			connection.rollbackTransaction();
			throw e;
		}
		catch (Exception e) {
			connection.rollbackTransaction();
			throw new RuntimeException(e);
		}
	}

	private void setUnitPrice(Entity track) {
		value(InvoiceLine.UNITPRICE).set(track == null ? null : track.get(Track.UNITPRICE));
	}

	private void updateTotals(Collection<Entity> invoiceLines, EntityConnection connection) throws DatabaseException {
		totalsUpdatedEvent.accept(connection.execute(Invoice.UPDATE_TOTALS, Entity.distinct(InvoiceLine.INVOICE_ID, invoiceLines)));
	}
}
