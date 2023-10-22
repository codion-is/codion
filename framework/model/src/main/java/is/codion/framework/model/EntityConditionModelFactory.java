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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static is.codion.framework.model.EntitySearchConditionModel.entitySearchConditionModel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link ColumnConditionModel.Factory} implementation for creating condition models.
 */
public class EntityConditionModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

  private final EntityConnectionProvider connectionProvider;

  /**
   * Instantiates a new {@link EntityConditionModelFactory}.
   * @param connectionProvider the connection provider
   */
  public EntityConditionModelFactory(EntityConnectionProvider connectionProvider) {
    this.connectionProvider = requireNonNull(connectionProvider);
  }

  @Override
  public Optional<ColumnConditionModel<? extends Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      return Optional.of(entitySearchConditionModel(foreignKey,
              EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider).build()));
    }

    ColumnDefinition<?> column = definition(attribute.entityType()).columns().definition((Column<?>) attribute);

    return Optional.of(ColumnConditionModel.builder(attribute, attribute.type().valueClass())
            .operators(operators(attribute))
            .format(column.format())
            .dateTimePattern(column.dateTimePattern())
            .build());
  }

  /**
   * @return the underlying connection provider
   */
  protected final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @param entityType the entity type
   * @return the entity definition
   */
  protected final EntityDefinition definition(EntityType entityType) {
    return connectionProvider.entities().definition(entityType);
  }

  private static List<Operator> operators(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL);
    }
    if (attribute.type().isBoolean()) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
