package org.jminor.framework.tools;

import org.jminor.common.db.exception.DbException;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * A static helper class for mass data manipulation.
 */
public final class EntityDataUtil {

  private EntityDataUtil() {}

  /**
   * Copies the given entities from source to destination
   * @param source the source db
   * @param destination the destination db
   * @param transactionBatchSize the number of records to copy between commits
   * @param copyPrimaryKeys if true primary key values are included, if false then they are assumed to be auto-generated
   * @param entityIDs the ID's of the entity types to copy
   * @throws DbException in case of a db exception
   */
  public static void copyEntities(final EntityDb source, final EntityDb destination, final int transactionBatchSize,
                                  final boolean copyPrimaryKeys, final String... entityIDs) throws DbException {
    for (final String entityID : entityIDs) {
      final List<Entity> entitiesToCopy = source.selectMany(EntityCriteriaUtil.selectCriteria(entityID).setForeignKeyFetchDepthLimit(0));
      if (!copyPrimaryKeys) {
        for (final Entity entity : entitiesToCopy) {
          entity.getPrimaryKey().clear();
        }
      }
      int fromIndex = 0;
      int toIndex = 0;
      while (fromIndex < entitiesToCopy.size()) {
        toIndex = Math.min(toIndex + transactionBatchSize, entitiesToCopy.size());
        final List<Entity> subList = entitiesToCopy.subList(fromIndex, toIndex);
        fromIndex = toIndex;
        destination.beginTransaction();
        destination.insert(subList);
        destination.commitTransaction();
      }
    }
  }
}