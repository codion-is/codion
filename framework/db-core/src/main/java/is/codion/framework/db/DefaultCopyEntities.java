/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection.CopyEntities;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultCopyEntities implements CopyEntities {

  private final EntityConnection source;
  private final EntityConnection destination;
  private final List<EntityType> entityTypes;

  private final Map<EntityType, Condition> conditions = new HashMap<>();

  private int batchSize = 100;
  private boolean includePrimaryKeys = true;

  DefaultCopyEntities(final EntityConnection source, final EntityConnection destination, final List<EntityType> entityTypes) {
    this.source = requireNonNull(source, "source");
    this.destination = requireNonNull(destination, "destination");
    this.entityTypes = requireNonNull(entityTypes);
  }

  private DefaultCopyEntities(final DefaultCopyEntities copyEntities) {
    this.source = copyEntities.source;
    this.destination = copyEntities.destination;
    this.entityTypes = copyEntities.entityTypes;
    this.conditions.putAll(copyEntities.conditions);
    this.batchSize = copyEntities.batchSize;
    this.includePrimaryKeys = copyEntities.includePrimaryKeys;
  }

  @Override
  public CopyEntities batchSize(final int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be a positive integer: " + batchSize);
    }
    DefaultCopyEntities copyEntities = new DefaultCopyEntities(this);
    copyEntities.batchSize = batchSize;

    return copyEntities;
  }

  @Override
  public CopyEntities includePrimaryKeys(final boolean includePrimaryKeys) {
    DefaultCopyEntities copyEntities = new DefaultCopyEntities(this);
    copyEntities.includePrimaryKeys = includePrimaryKeys;

    return copyEntities;
  }

  @Override
  public CopyEntities condition(final EntityType entityType, final Condition condition) {
    requireNonNull(entityType);
    requireNonNull(condition);
    DefaultCopyEntities copyEntities = new DefaultCopyEntities(this);
    copyEntities.conditions.put(entityType, condition);

    return copyEntities;
  }

  @Override
  public void execute() throws DatabaseException {
    for (final EntityType entityType : entityTypes) {
      List<Entity> entities = source.select(conditions.getOrDefault(entityType, Conditions.condition(entityType))
              .toSelectCondition()
              .fetchDepth(0));
      if (!includePrimaryKeys) {
        entities.forEach(Entity::clearPrimaryKey);
      }
      new DefaultInsertEntities(destination, entities.iterator())
              .batchSize(batchSize)
              .execute();
    }
  }
}
