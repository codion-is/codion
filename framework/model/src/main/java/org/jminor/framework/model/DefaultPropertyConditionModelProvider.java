/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Property;

/**
 * A default PropertyConditionModelProvider implementation.
 */
public class DefaultPropertyConditionModelProvider implements PropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<Property.ColumnProperty> initializePropertyConditionModel(
          final Property.ColumnProperty property) {
    return new DefaultPropertyConditionModel(property);
  }

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<Property.ForeignKeyProperty> initializeForeignKeyConditionModel(
          final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getForeignEntityId(), connectionProvider);
    lookupModel.getMultipleSelectionAllowedValue().set(true);

    return new DefaultForeignKeyConditionModel(property, lookupModel);
  }
}
