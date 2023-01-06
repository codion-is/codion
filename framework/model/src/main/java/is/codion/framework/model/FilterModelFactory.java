/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.Property;

/**
 * Responsible for creating {@link ColumnConditionModel} based on properties.
 */
public interface FilterModelFactory {

  /**
   * Creates a {@link ColumnConditionModel} for the given property
   * @param <T> the column value type
   * @param property the Property for which to create a {@link ColumnConditionModel}
   * @return a {@link ColumnConditionModel} for the given property, null if filtering should
   * not be allowed for this property
   */
  <T> ColumnConditionModel<Attribute<T>, T> createFilterModel(Property<T> property);
}
