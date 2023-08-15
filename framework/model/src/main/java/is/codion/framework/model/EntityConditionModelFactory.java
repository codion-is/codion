/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

import static is.codion.framework.model.EntitySearchModelConditionModel.entitySearchModelConditionModel;
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
      return Optional.of(entitySearchModelConditionModel(foreignKey,
              EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider).build()));
    }

    ColumnDefinition<?> definition = definition(attribute.entityType()).columnDefinition((Column<?>) attribute);
    if (definition.isAggregateColumn()) {
      return Optional.empty();
    }

    return Optional.ofNullable(ColumnConditionModel.builder(attribute, attribute.valueClass())
            .operators(operators(attribute))
            .format(definition.format())
            .dateTimePattern(definition.dateTimePattern())
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
    if (attribute.isBoolean()) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
