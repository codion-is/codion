/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
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
  public PropertyConditionModel<Property.ForeignKeyProperty> initializeForeignKeyConditionModel(
          final Property.ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().isSmallDataset(foreignKeyProperty.getForeignEntityID())) {
      final EntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityID(), connectionProvider);
      comboBoxModel.setNullValue(connectionProvider.getEntities().createToStringEntity(foreignKeyProperty.getForeignEntityID(), ""));

      return new SwingForeignKeyConditionModel(connectionProvider.getConditions(), foreignKeyProperty, comboBoxModel);
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
