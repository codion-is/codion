/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import static java.util.Objects.requireNonNull;

/**
 * A default ConditionModelFactory implementation.
 */
public class DefaultConditionModelFactory implements ConditionModelFactory {

  private final EntityConnectionProvider connectionProvider;

  public DefaultConditionModelFactory(final EntityConnectionProvider connectionProvider) {
    this.connectionProvider = requireNonNull(connectionProvider);
  }

  @Override
  public <T, A extends Attribute<T>> ColumnConditionModel<A, T> createConditionModel(final A attribute) {
    if (attribute instanceof ForeignKey) {
      final ForeignKey foreignKey = (ForeignKey) attribute;
      final EntitySearchModel searchModel = new DefaultEntitySearchModel(foreignKey.getReferencedEntityType(), connectionProvider);
      searchModel.getMultipleSelectionEnabledValue().set(true);

      return (ColumnConditionModel<A, T>) new DefaultForeignKeyConditionModel(foreignKey, searchModel);
    }

    final ColumnProperty<T> property = getColumnProperty(attribute);
    if (property.isAggregateColumn()) {
      return null;
    }

    return new DefaultColumnConditionModel<>(attribute, attribute.getTypeClass(),
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
  protected final EntityDefinition getDefinition(final EntityType entityType) {
    return connectionProvider.getEntities().getDefinition(entityType);
  }

  /**
   * @param <T> the attribute type
   * @param attribute the attribute
   * @return the ColumnProperty based on the given attribute
   */
  protected final <T> ColumnProperty<T> getColumnProperty(final Attribute<T> attribute) {
    return connectionProvider.getEntities().getDefinition(attribute.getEntityType()).getColumnProperty(attribute);
  }

  /**
   * @param foreignKey the foreign key
   * @return the ForeignKeyProperty based on the given foreign key
   */
  protected final ForeignKeyProperty getForeignKeyProperty(final ForeignKey foreignKey) {
    return connectionProvider.getEntities().getDefinition(foreignKey.getEntityType()).getForeignKeyProperty(foreignKey);
  }
}
