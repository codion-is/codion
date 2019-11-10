/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;

/**
 * Specifies an object responsible for providing property condition models
 */
public interface PropertyConditionModelProvider {

  /**
   * Initializes a PropertyConditionModel for the given property
   * @param property the Property for which to create a PropertyConditionModel
   * @return a PropertyConditionModel for the given property, null if searching
   * should not be allowed for this property
   */
  PropertyConditionModel<ColumnProperty> initializePropertyConditionModel(
          final ColumnProperty property);

  /**
   * Initializes a PropertyConditionModel for the given property
   * @param property the Property for which to create a PropertyConditionModel
   * @param connectionProvider the EntityConnectionProvider instance to use
   * @return a PropertyConditionModel for the given property, null if searching
   * should not be allowed for this property
   */
  PropertyConditionModel<ForeignKeyProperty> initializeForeignKeyConditionModel(
          final ForeignKeyProperty property, final EntityConnectionProvider connectionProvider);
}
