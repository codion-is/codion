/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * Specifies an object responsible for providing property filter models
 */
public interface PropertyFilterModelProvider {

  /**
   * Initializes a ColumnConditionModel for the given property
   * @param property the Property for which to initialize a ColumnConditionModel
   * @return a ColumnConditionModel for the given property
   */
  ColumnConditionModel<Entity, Property> initializePropertyFilterModel(Property property);
}
