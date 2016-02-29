/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.ProgressReporter;
import org.jminor.common.model.Util;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * A static helper class for mass data manipulation.
 */
public final class EntityConnectionUtil {

  private EntityConnectionUtil() {}

  /**
   * Copies the given entities from source to destination
   * @param source the source db
   * @param destination the destination db
   * @param batchSize the number of records to copy between commits
   * @param includePrimaryKeys if true primary key values are included, if false then they are assumed to be auto-generated
   * @param entityIDs the IDs of the entity types to copy
   * @throws DatabaseException in case of a db exception
   * @throws IllegalArgumentException if <code>batchSize</code> is not a positive integer
   */
  public static void copyEntities(final EntityConnection source, final EntityConnection destination, final int batchSize,
                                  final boolean includePrimaryKeys, final String... entityIDs) throws DatabaseException {
    for (final String entityID : entityIDs) {
      final List<Entity> entities = source.selectMany(EntityCriteriaUtil.selectCriteria(entityID).setForeignKeyFetchDepthLimit(0));
      if (!includePrimaryKeys) {
        for (final Entity entity : entities) {
          entity.clearPrimaryKeyValues();
        }
      }
      batchInsert(destination, entities, null, batchSize, null);
    }
  }

  /**
   * Inserts the given entities, performing a commit after each <code>batchSize</code> number of inserts.
   * @param connection the entity connection to use when inserting
   * @param entities the entities to insert
   * @param committed after the call this list will contain the primary keys of successfully inserted entities
   * @param batchSize the commit batch size
   * @param progressReporter if specified this will be used to report batch progress
   * @throws DatabaseException in case of an exception
   * @throws IllegalArgumentException if <code>batchSize</code> is not a positive integer
   */
  public static void batchInsert(final EntityConnection connection, final List<Entity> entities,
                                 final List<Entity.Key> committed, final int batchSize,
                                 final ProgressReporter progressReporter) throws DatabaseException {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
    }
    if (Util.nullOrEmpty(entities)) {
      return;
    }
    int insertedCount = 0;
    for (int i = 0; i < entities.size(); i += batchSize) {
      final List<Entity> insertBatch = entities.subList(i, Math.min(i + batchSize, entities.size()));
      final List<Entity.Key> insertedKeys = connection.insert(insertBatch);
      insertedCount += insertedKeys.size();
      if (committed != null) {
        committed.addAll(insertedKeys);
      }
      if (progressReporter != null) {
        progressReporter.reportProgress(insertedCount);
      }
    }
  }
}