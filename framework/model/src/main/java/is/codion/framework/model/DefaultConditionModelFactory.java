/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default ConditionModelFactory implementation.
 */
public class DefaultConditionModelFactory implements ConditionModelFactory {

  private final EntityConnectionProvider connectionProvider;

  public DefaultConditionModelFactory(EntityConnectionProvider connectionProvider) {
    this.connectionProvider = requireNonNull(connectionProvider);
  }

  @Override
  public <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(A attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      return (ColumnConditionModel<A, T>) new DefaultForeignKeyConditionModel(foreignKey,
              new DefaultEntitySearchModel(foreignKey.getReferencedEntityType(), connectionProvider));
    }

    ColumnProperty<T> property = getDefinition(attribute.getEntityType()).getColumnProperty(attribute);
    if (property.isAggregateColumn()) {
      return null;
    }

    return new DefaultColumnConditionModel<>(attribute, attribute.getTypeClass(), getOperators(attribute),
            Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimePattern());
  }

  /**
   * @return the underlying connection provider
   */
  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * @param entityType the entity type
   * @return the entity definition
   */
  protected final EntityDefinition getDefinition(EntityType entityType) {
    return connectionProvider.getEntities().getDefinition(entityType);
  }

  private static List<Operator> getOperators(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL);
    }
    if (attribute.isBoolean()) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
