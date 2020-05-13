/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.javafx.framework.model;

import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.model.DefaultPropertyConditionModelProvider;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link EntityDefinition#isSmallDataset()}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      //todo comboBoxModel.setNullValue(Domain.createToStringEntity(property.getForeignEntityId(), ""));
      return new FXForeignKeyConditionListModel(foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getForeignEntityId(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
