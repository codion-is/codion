/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyCriteriaModelProvider;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.PropertyCriteriaModel;

/**
 * A Swing {@link org.jminor.framework.model.PropertyCriteriaModelProvider} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingPropertyCriteriaModelProvider extends DefaultPropertyCriteriaModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyCriteriaModel<? extends Property.SearchableProperty> initializePropertyCriteriaModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
        final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
        comboBoxModel.setNullValue(EntityUtil.createToStringEntity(foreignKeyProperty.getReferencedEntityID(), ""));

        return new SwingForeignKeyCriteriaModel(foreignKeyProperty, comboBoxModel);
      }
    }

    return super.initializePropertyCriteriaModel(property, connectionProvider);
  }
}
