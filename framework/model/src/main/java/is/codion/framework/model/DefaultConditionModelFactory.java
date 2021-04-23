/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

/**
 * A default ConditionModelFactory implementation.
 */
public class DefaultConditionModelFactory implements ConditionModelFactory {

  @Override
  public <T> ColumnConditionModel<Attribute<?>, T> createColumnConditionModel(final ColumnProperty<T> property) {
    return new DefaultColumnConditionModel<>(property.getAttribute(), property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(),
            property.getFormat(), property.getDateTimePattern());
  }

  @Override
  public ColumnConditionModel<ForeignKey, Entity> createForeignKeyConditionModel(
          final ForeignKey foreignKey, final EntityConnectionProvider connectionProvider) {
    final EntitySearchModel searchModel = new DefaultEntitySearchModel(foreignKey.getReferencedEntityType(), connectionProvider);
    searchModel.getMultipleSelectionEnabledValue().set(true);

    return new DefaultForeignKeyConditionModel(foreignKey, searchModel);
  }
}
