/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.PropertyConditionModelProvider;

/**
 * A Swing {@link PropertyConditionModelProvider} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingPropertyConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityId(), connectionProvider);
      comboBoxModel.setNullValue(connectionProvider.getDomain().createToStringEntity(foreignKeyProperty.getForeignEntityId(), ""));

      return new SwingForeignKeyConditionModel(foreignKeyProperty, comboBoxModel);
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
