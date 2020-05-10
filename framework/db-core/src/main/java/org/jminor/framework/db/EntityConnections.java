/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.selectCondition;

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
   * @param entityIds the IDs of the entity types to copy
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
   */
  public static void copyEntities(final EntityConnection source, final EntityConnection destination, final int batchSize,
                                  final IncludePrimaryKeys includePrimaryKeys, final String... entityIds) throws DatabaseException {
    requireNonNull(source, "source");
    requireNonNull(destination, "destination");
    requireNonNull(includePrimaryKeys, "includePrimaryKeys");
    requireNonNull(entityIds);
    for (final String entityId : entityIds) {
      final List<Entity> entities = source.select(selectCondition(entityId).setForeignKeyFetchDepth(0));
      if (includePrimaryKeys == IncludePrimaryKeys.NO) {
        entities.forEach(Entity::clearKeyValues);
      }
      batchInsert(destination, entities, batchSize, null);
    }
  }

  /**
   * Inserts the given entities, performing a commit after each {@code batchSize} number of inserts.
   * @param connection the entity connection to use when inserting
   * @param entities the entities to insert
   * @param batchSize the commit batch size
   * @param progressReporter if specified this will be used to report batch progress
   * @return the primary keys of successfully inserted entities
   * @throws DatabaseException in case of an exception
   * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
   */
  public static List<Entity.Key> batchInsert(final EntityConnection connection, final List<Entity> entities,
                                             final int batchSize, final EventDataListener<Integer> progressReporter)
          throws DatabaseException {
    requireNonNull(connection, "connection");
    requireNonNull(entities, "entities");
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
    }
    if (Util.nullOrEmpty(entities)) {
      return Collections.emptyList();
    }
    final List<Entity.Key> insertedKeys = new ArrayList<>();
    for (int i = 0; i < entities.size(); i += batchSize) {
      final List<Entity> insertBatch = entities.subList(i, Math.min(i + batchSize, entities.size()));
      insertedKeys.addAll(connection.insert(insertBatch));
      if (progressReporter != null) {
        progressReporter.onEvent(insertedKeys.size());
      }
    }

    return insertedKeys;
  }
}