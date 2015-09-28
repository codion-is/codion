/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

/**
 * A default PropertySearchModelProvider implementation.
 */
public class DefaultPropertySearchModelProvider implements PropertySearchModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return initializeForeignKeySearchModel((Property.ForeignKeyProperty) property, connectionProvider);
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertySearchModel((Property.ColumnProperty) property);
    }

    throw new IllegalArgumentException("Not a searchable property (Property.ColumnProperty or Property.ForeignKeyProperty): " + property);
  }

  private PropertySearchModel<? extends Property.SearchableProperty> initializeForeignKeySearchModel(
          final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    if (Entities.isSmallDataset(property.getReferencedEntityID())) {
      final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(property.getReferencedEntityID(), connectionProvider);
      comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));

      return new DefaultForeignKeySearchModel(property, comboBoxModel);
    }
    else {
      final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getReferencedEntityID(),
              connectionProvider, Entities.getSearchProperties(property.getReferencedEntityID()));
      lookupModel.getMultipleSelectionAllowedValue().set(true);

      return new DefaultForeignKeySearchModel(property, lookupModel);
    }
  }
}
