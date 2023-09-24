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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
    addEditListener(InvoiceLine.TRACK_FK, this::setUnitPrice);
  }

  void addTotalsUpdatedListener(Consumer<Collection<Entity>> listener) {
    totalsUpdatedEvent.addDataListener(listener);
  }

  @Override
  protected Collection<Entity> doInsert(Collection<? extends Entity> entities) throws DatabaseException {
    EntityConnection connection = connectionProvider().connection();
    connection.beginTransaction();
    try {
      Collection<Entity> inserted = connection.insertSelect(entities);
      updateTotals(entities, connection);
      connection.commitTransaction();

      return inserted;
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  @Override
  protected Collection<Entity> doUpdate(Collection<? extends Entity> entities) throws DatabaseException {
    EntityConnection connection = connectionProvider().connection();
    connection.beginTransaction();
    try {
      Collection<Entity> updated = connection.updateSelect(entities);
      updateTotals(entities, connection);
      connection.commitTransaction();

      return updated;
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  @Override
  protected void doDelete(Collection<? extends Entity> entities) throws DatabaseException {
    EntityConnection connection = connectionProvider().connection();
    connection.beginTransaction();
    try {
      connection.delete(Entity.primaryKeys(entities));
      updateTotals(entities, connection);
      connection.commitTransaction();
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  private void setUnitPrice(Entity track) {
    put(InvoiceLine.UNITPRICE, track == null ? null : track.get(Track.UNITPRICE));
  }

  private void updateTotals(Collection<? extends Entity> entities, EntityConnection connection) throws DatabaseException {
    totalsUpdatedEvent.accept(connection.execute(Invoice.UPDATE_TOTALS, Entity.distinct(InvoiceLine.INVOICE_ID, entities)));
  }
}
