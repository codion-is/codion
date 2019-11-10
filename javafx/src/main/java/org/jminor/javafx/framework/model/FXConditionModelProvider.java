/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.PropertyConditionModel;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link org.jminor.framework.domain.Entity.Definition#isSmallDataset()}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      //todo comboBoxModel.setNullValue(Domain.createToStringEntity(property.getForeignEntityId(), ""));
      return new FXForeignKeyConditionListModel(foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getForeignEntityId(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
