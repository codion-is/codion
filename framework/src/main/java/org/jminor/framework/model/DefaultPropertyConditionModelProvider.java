/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

/**
 * A default PropertyConditionModelProvider implementation.
 */
public class DefaultPropertyConditionModelProvider implements PropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<? extends Property.SearchableProperty> initializePropertyConditionModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return initializeForeignKeyConditionModel((Property.ForeignKeyProperty) property, connectionProvider);
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertyConditionModel((Property.ColumnProperty) property);
    }

    throw new IllegalArgumentException("Not a searchable property (Property.ColumnProperty or Property.ForeignKeyProperty): " + property);
  }

  private PropertyConditionModel<? extends Property.SearchableProperty> initializeForeignKeyConditionModel(
          final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {

    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getReferencedEntityID(),
            connectionProvider, Entities.getSearchProperties(property.getReferencedEntityID()));
    lookupModel.getMultipleSelectionAllowedValue().set(true);

    return new DefaultForeignKeyConditionModel(property, lookupModel);
  }
}
