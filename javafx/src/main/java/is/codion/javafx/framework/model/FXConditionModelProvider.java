/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultPropertyConditionModelProvider;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link EntityDefinition#isSmallDataset()}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().getDefinition(foreignKeyProperty.getReferencedEntityType()).isSmallDataset()) {
      return new FXForeignKeyConditionListModel(foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getReferencedEntityType(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
