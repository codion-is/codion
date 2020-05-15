/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;

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
