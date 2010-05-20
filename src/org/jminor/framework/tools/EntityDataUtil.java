package org.jminor.framework.tools;

import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;

import java.util.List;

public class EntityDataUtil {

  public static void copyEntities(final EntityDb source, final EntityDb destination, final int transactionBatchSize,
                                  final boolean copyPrimaryKeys, final String... entityIDs) throws Exception {
    try {
      for (final String entityID : entityIDs) {
        final List<Entity> entitiesToCopy = source.selectMany(new EntitySelectCriteria(entityID).setFetchDepthForAll(0));
        if (!copyPrimaryKeys) {
          for (final Entity entity : entitiesToCopy)
            entity.getPrimaryKey().clear();
        }
        int fromIndex = 0, toIndex = 0;
        while (fromIndex < entitiesToCopy.size()) {
          toIndex = Math.min(toIndex + transactionBatchSize, entitiesToCopy.size());
          System.out.println("Copying " + entityID + " from " + fromIndex + " to " + toIndex);
          final List<Entity> subList = entitiesToCopy.subList(fromIndex, toIndex);
          fromIndex = toIndex;
          destination.beginTransaction();
          destination.insert(subList);
          destination.commitTransaction();
        }
      }
    }
    catch (Exception e) {
      try {
        if (destination.isTransactionOpen())
          destination.rollbackTransaction();
      }
      catch (Exception e1) {/**/}
      throw e;
    }
  }
}
