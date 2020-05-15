/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model;

import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.model.table.DefaultColumnConditionModel;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.domain.property.Property;

/**
 * A default PropertyConditionModelProvider implementation.
 */
public class DefaultPropertyConditionModelProvider implements PropertyConditionModelProvider {

  @Override
  public ColumnConditionModel<Entity, ColumnProperty> initializePropertyConditionModel(final ColumnProperty property) {
    return new DefaultColumnConditionModel<>(property, property.getTypeClass(), Property.WILDCARD_CHARACTER.get(),
            property.getFormat(), property.getDateTimeFormatPattern());
  }

  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getForeignEntityId(), connectionProvider);
    lookupModel.getMultipleSelectionEnabledValue().set(true);

    return new DefaultForeignKeyConditionModel(property, lookupModel);
  }
}
