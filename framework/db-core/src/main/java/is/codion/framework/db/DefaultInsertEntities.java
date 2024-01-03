/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection.Insert;
import is.codion.framework.domain.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultInsertEntities implements Insert {

  private final EntityConnection connection;
  private final Iterator<Entity> entityIterator;
  private final int batchSize;
  private final Consumer<Integer> progressReporter;
  private final Consumer<Collection<Entity.Key>> onInsert;

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
      Collection<Entity.Key> insertedKeys = connection.insert(batch);
      progress += insertedKeys.size();
      batch.clear();
      if (progressReporter != null) {
        progressReporter.accept(progress);
      }
      if (onInsert != null) {
        onInsert.accept(insertedKeys);
      }
    }
  }

  static final class DefaultBuilder implements Builder {

    private final EntityConnection connection;
    private final Iterator<Entity> entityIterator;

    private int batchSize = 100;
    private Consumer<Integer> progressReporter;
    private Consumer<Collection<Entity.Key>> onInsert;

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
    public Builder progressReporter(Consumer<Integer> progressReporter) {
      this.progressReporter = requireNonNull(progressReporter);
      return this;
    }

    @Override
    public Builder onInsert(Consumer<Collection<Entity.Key>> onInsert) {
      this.onInsert = requireNonNull(onInsert);
      return this;
    }

    @Override
    public void execute() throws DatabaseException {
      build().execute();
    }

    @Override
    public Insert build() {
      return new DefaultInsertEntities(this);
    }
  }
}
