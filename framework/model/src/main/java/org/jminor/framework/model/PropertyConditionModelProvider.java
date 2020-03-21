/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;

/**
 * Specifies an object responsible for providing property condition models
 */
public interface PropertyConditionModelProvider {

  /**
   * Initializes a {@link ColumnConditionModel} for the given property
   * @param property the Property for which to create a {@link ColumnConditionModel}
   * @return a {@link ColumnConditionModel} for the given property, null if searching
   * should not be allowed for this property
   */
  ColumnConditionModel<Entity, ColumnProperty> initializePropertyConditionModel(ColumnProperty property);

  /**
   * Initializes a {@link ColumnConditionModel} for the given property
   * @param property the Property for which to create a {@link ColumnConditionModel}
   * @param connectionProvider the EntityConnectionProvider instance to use
   * @return a {@link ColumnConditionModel} for the given property, null if searching
   * should not be allowed for this property
   */
  ColumnConditionModel<Entity, ForeignKeyProperty> initializeForeignKeyConditionModel(
          ForeignKeyProperty property, EntityConnectionProvider connectionProvider);
}
