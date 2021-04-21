/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultConditionModelFactory;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link EntityDefinition#isSmallDataset()}
 */
public class FXConditionModelFactory extends DefaultConditionModelFactory {

  @Override
  public ColumnConditionModel<ForeignKey, Entity> createForeignKeyConditionModel(
          final ForeignKey foreignKey, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      return new FXForeignKeyConditionListModel(foreignKey,
              new ObservableEntityList(foreignKey.getReferencedEntityType(), connectionProvider));
    }

    return super.createForeignKeyConditionModel(foreignKey, connectionProvider);
  }
}
