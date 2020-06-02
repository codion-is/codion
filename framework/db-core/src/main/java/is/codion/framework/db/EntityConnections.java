/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static java.util.Objects.requireNonNull;

/**
 * A static helper class for mass data manipulation.
 */
public final class EntityConnections {

  /**
   * Specifies whether primary key values should be included when copying entities.
   */
  public enum IncludePrimaryKeys {
    /**
     * Primary key values should be include during a copy operation.
     */
    YES,
    /**
     * Primary key values should not be include during a copy operation.
     */
    NO
  }

  private EntityConnections() {}

  /**
   * Copies the given entities from source to destination
   * @param source the source db
   * @param destination the destination db
   * @param batchSize the number of records to copy between commits
   * @param includePrimaryKeys specifies whether primary key values should be included when copying
   * @param entityIds the ids of the entity types to copy
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
   */
  public static void copyEntities(final EntityConnection source, final EntityConnection destination, final int batchSize,
                                  final IncludePrimaryKeys includePrimaryKeys, final EntityId... entityIds) throws DatabaseException {
    requireNonNull(source, "source");
    requireNonNull(destination, "destination");
    requireNonNull(includePrimaryKeys, "includePrimaryKeys");
    requireNonNull(entityIds);
    for (final EntityId entityId : entityIds) {
      final List<Entity> entities = source.select(selectCondition(entityId).setForeignKeyFetchDepth(0));
      if (includePrimaryKeys == IncludePrimaryKeys.NO) {
        entities.forEach(Entity::clearKeyValues);
      }
      batchInsert(destination, entities.iterator(), batchSize, null, null);
    }
  }

  /**
   * Inserts the given entities, performing a commit after each {@code batchSize} number of inserts,
   * unless the connection has an open transaction.
   * @param connection the entity connection to use when inserting
   * @param entities the entities to insert
   * @param batchSize the commit batch size
   * @param progressReporter if specified this will be used to report batch progress
   * @param onInsertBatchListener notified each time a batch is inserted, providing the inserted keys
   * @throws DatabaseException in case of an exception
   * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
   */
  public static void batchInsert(final EntityConnection connection, final Iterator<Entity> entities, final int batchSize,
                                 final EventDataListener<Integer> progressReporter,
                                 final EventDataListener<List<Entity.Key>> onInsertBatchListener)
          throws DatabaseException {
    requireNonNull(connection, "connection");
    requireNonNull(entities, "entities");
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
    }
    final List<Entity> batch = new ArrayList<>(batchSize);
    int progress = 0;
    while (entities.hasNext()) {
      while (batch.size() < batchSize && entities.hasNext()) {
        batch.add(entities.next());
      }
      final List<Entity.Key> insertedKeys = connection.insert(batch);
      progress += insertedKeys.size();
      batch.clear();
      if (progressReporter != null) {
        progressReporter.onEvent(progress);
      }
      if (onInsertBatchListener != null) {
        onInsertBatchListener.onEvent(insertedKeys);
      }
    }
  }
}