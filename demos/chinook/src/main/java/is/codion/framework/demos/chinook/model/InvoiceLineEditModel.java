/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Collection;
import java.util.List;

public final class InvoiceLineEditModel extends SwingEntityEditModel {

  private final Event<Collection<Entity>> totalsUpdatedEvent = Event.event();

  public InvoiceLineEditModel(final EntityConnectionProvider connectionProvider) {
    super(InvoiceLine.TYPE, connectionProvider);
  }

  void addTotalsUpdatedListener(final EventDataListener<Collection<Entity>> listener) {
    totalsUpdatedEvent.addDataListener(listener);
  }

  @Override
  protected List<Key> doInsert(final List<Entity> entities) throws DatabaseException {
    final EntityConnection connection = getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      final List<Key> keys = connection.insert(entities);
      updateTotals(entities, connection);
      connection.commitTransaction();

      return keys;
    }
    catch (final DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  @Override
  protected List<Entity> doUpdate(final List<Entity> entities) throws DatabaseException {
    final EntityConnection connection = getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      final List<Entity> updated = connection.update(entities);
      updateTotals(entities, connection);
      connection.commitTransaction();

      return updated;
    }
    catch (final DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  @Override
  protected List<Entity> doDelete(final List<Entity> entities) throws DatabaseException {
    final EntityConnection connection = getConnectionProvider().getConnection();
    connection.beginTransaction();
    try {
      connection.delete(Entity.getPrimaryKeys(entities));
      updateTotals(entities, connection);
      connection.commitTransaction();

      return entities;
    }
    catch (final DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
  }

  private void updateTotals(final List<Entity> entities, final EntityConnection connection) throws DatabaseException {
    totalsUpdatedEvent.onEvent(connection.executeFunction(Invoice.UPDATE_TOTALS, Entity.get(InvoiceLine.INVOICE_ID, entities)));
  }
}
