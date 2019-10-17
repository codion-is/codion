/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.PropertyConditionModel;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link Domain#isSmallDataset(String)}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<Property.ForeignKeyProperty> initializeForeignKeyConditionModel(
          final Property.ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getDomain().isSmallDataset(foreignKeyProperty.getForeignEntityId())) {
      //todo comboBoxModel.setNullValue(Domain.createToStringEntity(property.getForeignEntityId(), ""));
      return new FXForeignKeyConditionListModel(foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getForeignEntityId(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
