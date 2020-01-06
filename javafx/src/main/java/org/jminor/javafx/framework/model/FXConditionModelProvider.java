/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link EntityDefinition#isSmallDataset()}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      //todo comboBoxModel.setNullValue(Domain.createToStringEntity(property.getForeignEntityId(), ""));
      return new FXForeignKeyConditionListModel(foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getForeignEntityId(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
