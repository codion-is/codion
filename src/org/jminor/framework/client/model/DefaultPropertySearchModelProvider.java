/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class DefaultPropertySearchModelProvider implements PropertySearchModelProvider {

  public PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityDbProvider dbProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty fkProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isLargeDataset(fkProperty.getReferencedEntityID())) {
        final EntityLookupModel lookupModel = new DefaultEntityLookupModel(fkProperty.getReferencedEntityID(),
                dbProvider, getSearchProperties(fkProperty.getReferencedEntityID()));
        lookupModel.setMultipleSelectionAllowed(true);
        return new DefaultForeignKeySearchModel(fkProperty, lookupModel);
      }
      else {
        final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(fkProperty.getReferencedEntityID(), dbProvider);
        comboBoxModel.setNullValueString("");
        return new DefaultForeignKeySearchModel(fkProperty, comboBoxModel);
      }
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertySearchModel((Property.ColumnProperty) property);
    }

    throw new RuntimeException("Not a searchable property (Property.ColumnProperty or PropertyForeignKeyProperty): " + property);
  }

  private List<Property.ColumnProperty> getSearchProperties(final String entityID) {
    final Collection<String> searchPropertyIDs = Entities.getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : Entities.getSearchProperties(entityID, searchPropertyIDs);
  }

  private List<Property.ColumnProperty> getStringProperties(final String entityID) {
    final Collection<Property.ColumnProperty> databaseProperties = Entities.getColumnProperties(entityID);
    final List<Property.ColumnProperty> stringProperties = new ArrayList<Property.ColumnProperty>();
    for (final Property.ColumnProperty property : databaseProperties) {
      if (property.isString()) {
        stringProperties.add(property);
      }
    }

    return stringProperties;
  }
}
