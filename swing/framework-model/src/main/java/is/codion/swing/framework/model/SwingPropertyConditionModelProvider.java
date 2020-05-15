/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultPropertyConditionModelProvider;
import is.codion.framework.model.PropertyConditionModelProvider;

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
