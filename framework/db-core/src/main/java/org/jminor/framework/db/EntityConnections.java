/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

/**
 * A static helper class for mass data manipulation.
 */
public final class EntityConnections {

  private EntityConnections() {}

  /**
   * Copies the given entities from source to destination
   * @param source the source db
   * @param destination the destination db
   * @param batchSize the number of records to copy between commits
   * @param includePrimaryKeys if true primary key values are included, if false then they are assumed to be auto-generated
   * @param entityIds the IDs of the entity types to copy
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException if {@code batchSize} is not a positive integer
   */
  public static void copyEntities(final EntityConnection source, final EntityConnection destination, final int batchSize,
                                  final boolean includePrimaryKeys, final String... entityIds) throws DatabaseException {
    for (final String entityId : entityIds) {
      final List<Entity> entities = source.select(entitySelectCondition(entityId).setForeignKeyFetchDepthLimit(0));
      if (!includePrimaryKeys) {
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