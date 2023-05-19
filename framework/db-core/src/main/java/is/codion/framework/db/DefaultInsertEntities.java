/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  private final int batchSize;
  private final EventDataListener<Integer> progressReporter;
  private final EventDataListener<List<Key>> onInsert;

  DefaultInsertEntities(DefaultBuilder builder) {
    this.connection = builder.connection;
    this.entityIterator = builder.entityIterator;
    this.batchSize = builder.batchSize;
    this.progressReporter = builder.progressReporter;
    this.onInsert = builder.onInsert;
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

  static final class DefaultBuilder implements Builder {

    private final EntityConnection connection;
    private final Iterator<Entity> entityIterator;

    private int batchSize = 100;
    private EventDataListener<Integer> progressReporter;
    private EventDataListener<List<Key>> onInsert;

    DefaultBuilder(EntityConnection connection, Iterator<Entity> entityIterator) {
      this.connection = requireNonNull(connection);
      this.entityIterator = requireNonNull(entityIterator);
    }

    @Override
    public Builder batchSize(int batchSize) {
      if (batchSize <= 0) {
        throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
      }
      this.batchSize = batchSize;
      return this;
    }

    @Override
    public Builder progressReporter(EventDataListener<Integer> progressReporter) {
      this.progressReporter = requireNonNull(progressReporter);
      return this;
    }

    @Override
    public Builder onInsert(EventDataListener<List<Key>> onInsert) {
      this.onInsert = requireNonNull(onInsert);
      return this;
    }

    @Override
    public void execute() throws DatabaseException {
      build().execute();
    }

    @Override
    public InsertEntities build() {
      return new DefaultInsertEntities(this);
    }
  }
}
