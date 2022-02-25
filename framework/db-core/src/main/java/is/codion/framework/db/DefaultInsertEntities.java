/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.framework.db.EntityConnection.InsertEntities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultInsertEntities implements InsertEntities {

  private final EntityConnection connection;
  private final Iterator<Entity> entityIterator;

  private int batchSize = 100;
  private EventDataListener<Integer> progressReporter;
  private EventDataListener<List<Key>> onInsert;

  DefaultInsertEntities(final EntityConnection connection, final Iterator<Entity> entityIterator) {
    this.connection = requireNonNull(connection);
    this.entityIterator = requireNonNull(entityIterator);
  }

  private DefaultInsertEntities(final DefaultInsertEntities insertEntities) {
    this.connection = insertEntities.connection;
    this.entityIterator = insertEntities.entityIterator;
    this.batchSize = insertEntities.batchSize;
    this.progressReporter = insertEntities.progressReporter;
    this.onInsert = insertEntities.onInsert;
  }

  @Override
  public InsertEntities batchSize(final int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
    }
    DefaultInsertEntities insertEntities = new DefaultInsertEntities(this);
    insertEntities.batchSize = batchSize;

    return insertEntities;
  }

  @Override
  public InsertEntities progressReporter(final EventDataListener<Integer> progressReporter) {
    requireNonNull(progressReporter);
    DefaultInsertEntities insertEntities = new DefaultInsertEntities(this);
    insertEntities.progressReporter = progressReporter;

    return insertEntities;
  }

  @Override
  public InsertEntities onInsert(final EventDataListener<List<Key>> onInsert) {
    requireNonNull(onInsert);
    DefaultInsertEntities insertEntities = new DefaultInsertEntities(this);
    insertEntities.onInsert = onInsert;

    return insertEntities;
  }

  @Override
  public void execute() throws DatabaseException {
    List<Entity> batch = new ArrayList<>(batchSize);
    int progress = 0;
    while (entityIterator.hasNext()) {
      while (batch.size() < batchSize && entityIterator.hasNext()) {
        batch.add(entityIterator.next());
      }
      List<Key> insertedKeys = connection.insert(batch);
      progress += insertedKeys.size();
      batch.clear();
      if (progressReporter != null) {
        progressReporter.onEvent(progress);
      }
      if (onInsert != null) {
        onInsert.onEvent(insertedKeys);
      }
    }
  }
}
