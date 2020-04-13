/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

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
