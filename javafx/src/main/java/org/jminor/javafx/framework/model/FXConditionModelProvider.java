/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.PropertyConditionModel;

/**
 * Provides foreign key condition models based on {@link ObservableEntityList} for
 * entities based on small datasets, see {@link Entities#isSmallDataset(String)}
 */
public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<Property.ForeignKeyProperty> initializeForeignKeyConditionModel(
          final Property.ForeignKeyProperty foreignKeyProperty, final EntityConnectionProvider connectionProvider) {
    if (connectionProvider.getEntities().isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
      //todo comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));
      return new FXForeignKeyConditionListModel(new EntityConditions(connectionProvider.getEntities()), foreignKeyProperty,
              new ObservableEntityList(foreignKeyProperty.getReferencedEntityID(), connectionProvider));
    }

    return super.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
  }
}
