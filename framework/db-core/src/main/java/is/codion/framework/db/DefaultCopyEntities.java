/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection.Copy;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

final class DefaultCopyEntities implements Copy {

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
      Condition entityCondition = conditions.get(entityType);
      Select.Builder conditionBuilder = entityCondition == null ?
              Select.all(entityType) :
              Select.where(entityCondition);
      List<Entity> entities = source.select(conditionBuilder
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
    public Copy build() {
      return new DefaultCopyEntities(this);
    }
  }
}
