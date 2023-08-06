/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection.CopyEntities;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.framework.db.condition.Condition.all;
import static java.util.Objects.requireNonNull;

final class DefaultCopyEntities implements CopyEntities {

  private final EntityConnection source;
  private final EntityConnection destination;
  private final Collection<EntityType> entityTypes = new ArrayList<>();
  private final Map<EntityType, Condition> conditions = new HashMap<>();
  private final int batchSize;
  private final boolean includePrimaryKeys;

  DefaultCopyEntities(DefaultBuilder builder) {
    this.source = builder.source;
    this.destination = builder.destination;
    this.entityTypes.addAll(builder.entityTypes);
    this.conditions.putAll(builder.conditions);
    this.batchSize = builder.batchSize;
    this.includePrimaryKeys = builder.includePrimaryKeys;
  }

  @Override
  public void execute() throws DatabaseException {
    for (EntityType entityType : entityTypes) {
      List<Entity> entities = source.select(conditions.getOrDefault(entityType, all(entityType))
              .selectBuilder()
              .fetchDepth(0)
              .build());
      if (!includePrimaryKeys) {
        entities.forEach(Entity::clearPrimaryKey);
      }
      new DefaultInsertEntities.DefaultBuilder(destination, entities.iterator())
              .batchSize(batchSize)
              .execute();
    }
  }

  static final class DefaultBuilder implements Builder {

    private final EntityConnection source;
    private final EntityConnection destination;
    private final Collection<EntityType> entityTypes = new ArrayList<>();
    private final Map<EntityType, Condition> conditions = new HashMap<>();

    private boolean includePrimaryKeys = true;
    private int batchSize = 100;

    DefaultBuilder(EntityConnection source, EntityConnection destination) {
      this.source = requireNonNull(source);
      this.destination = requireNonNull(destination);
    }

    @Override
    public Builder entityTypes(EntityType... entityTypes) {
      this.entityTypes.addAll(Arrays.asList(requireNonNull(entityTypes)));
      return this;
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
    public Builder includePrimaryKeys(boolean includePrimaryKeys) {
      this.includePrimaryKeys = includePrimaryKeys;
      return this;
    }

    @Override
    public Builder condition(Condition condition) {
      if (!entityTypes.contains(requireNonNull(condition).entityType())) {
        throw new IllegalArgumentException("CopyEntities.Builder does not contain entityType: " + condition.entityType());
      }
      this.conditions.put(condition.entityType(), condition);
      return this;
    }

    @Override
    public void execute() throws DatabaseException {
      build().execute();
    }

    @Override
    public CopyEntities build() {
      return new DefaultCopyEntities(this);
    }
  }
}
