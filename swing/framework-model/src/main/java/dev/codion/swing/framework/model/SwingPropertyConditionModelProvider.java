/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.model;

import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.model.DefaultPropertyConditionModelProvider;
import dev.codion.framework.model.PropertyConditionModelProvider;

/**
 * A Swing {@link PropertyConditionModelProvider} implementation
 * using ComboBoxModel for foreign key properties with small datasets
 */
public class SwingPropertyConditionModelProvider extends DefaultPropertyConditionModelProvider {

  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityId(), connectionProvider);
      comboBoxModel.setNullValue(connectionProvider.getEntities().createToStringEntity(foreignKeyProperty.getForeignEntityId(), ""));

      return new SwingForeignKeyConditionModel(foreignKeyProperty, comboBoxModel);
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
