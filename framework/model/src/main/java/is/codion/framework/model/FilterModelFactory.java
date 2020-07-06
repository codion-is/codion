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
public interface FilterModelFactory {

  /**
   * Initializes a ColumnConditionModel for the given property
   * @param <T> the column value type
   * @param property the Property for which to initialize a ColumnConditionModel
   * @return a ColumnConditionModel for the given property
   */
  <T> ColumnConditionModel<Entity, Property<?>, T> createFilterModel(Property<T> property);
}
