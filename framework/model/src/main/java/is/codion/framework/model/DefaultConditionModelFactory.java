/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

/**
 * A default ConditionModelFactory implementation.
 */
public class DefaultConditionModelFactory implements ConditionModelFactory {

  @Override
  public <T> ColumnConditionModel<Entity, ColumnProperty<?>, T> createColumnConditionModel(final ColumnProperty<T> property) {
    return new DefaultColumnConditionModel<>(property, property.getAttribute().getTypeClass(), Property.WILDCARD_CHARACTER.get(),
            property.getFormat(), property.getDateTimeFormatPattern());
  }

  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty, Entity> createForeignKeyConditionModel(
          final ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getReferencedEntityType(), connectionProvider);
    lookupModel.getMultipleSelectionEnabledValue().set(true);

    return new DefaultForeignKeyConditionModel(property, lookupModel);
  }
}
