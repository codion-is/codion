/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

/**
 * A default PropertyCriteriaModelProvider implementation.
 */
public class DefaultPropertyCriteriaModelProvider implements PropertyCriteriaModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyCriteriaModel<? extends Property.SearchableProperty> initializePropertyCriteriaModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      return initializeForeignKeyCriteriaModel((Property.ForeignKeyProperty) property, connectionProvider);
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertyCriteriaModel((Property.ColumnProperty) property);
    }

    throw new IllegalArgumentException("Not a searchable property (Property.ColumnProperty or Property.ForeignKeyProperty): " + property);
  }

  private PropertyCriteriaModel<? extends Property.SearchableProperty> initializeForeignKeyCriteriaModel(
          final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    if (Entities.isSmallDataset(property.getReferencedEntityID())) {
      final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(property.getReferencedEntityID(), connectionProvider);
      comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));

      return new DefaultForeignKeyCriteriaModel(property, comboBoxModel);
    }
    else {
      final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getReferencedEntityID(),
              connectionProvider, Entities.getSearchProperties(property.getReferencedEntityID()));
      lookupModel.getMultipleSelectionAllowedValue().set(true);

      return new DefaultForeignKeyCriteriaModel(property, lookupModel);
    }
  }
}
