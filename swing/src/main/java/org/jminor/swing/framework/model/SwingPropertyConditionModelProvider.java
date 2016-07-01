/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.PropertyConditionModel;
import org.jminor.framework.model.PropertyConditionModelProvider;

/**
 * A Swing {@link PropertyConditionModelProvider} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingPropertyConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<? extends Property.SearchableProperty> initializePropertyConditionModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
        final EntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
        comboBoxModel.setNullValue(EntityUtil.createToStringEntity(foreignKeyProperty.getReferencedEntityID(), ""));

        return new SwingForeignKeyConditionModel(foreignKeyProperty, comboBoxModel);
      }
    }

    return super.initializePropertyConditionModel(property, connectionProvider);
  }
}
