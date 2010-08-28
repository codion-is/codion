/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

/**
 * A default PropertySearchModelProvider implementation.
 */
public class DefaultPropertySearchModelProvider implements PropertySearchModelProvider {

  /** {@inheritDoc} */
  public PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityDbProvider dbProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty fkProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isSmallDataset(fkProperty.getReferencedEntityID())) {
        final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(fkProperty.getReferencedEntityID(), dbProvider);
        comboBoxModel.setNullValueString("");
        return new DefaultForeignKeySearchModel(fkProperty, comboBoxModel);
      }
      else {
        final EntityLookupModel lookupModel = new DefaultEntityLookupModel(fkProperty.getReferencedEntityID(),
                dbProvider, Entities.getSearchProperties(fkProperty.getReferencedEntityID()));
        lookupModel.setMultipleSelectionAllowed(true);
        return new DefaultForeignKeySearchModel(fkProperty, lookupModel);
      }
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertySearchModel((Property.ColumnProperty) property);
    }

    throw new IllegalArgumentException("Not a searchable property (Property.ColumnProperty or PropertyForeignKeyProperty): " + property);
  }
}
