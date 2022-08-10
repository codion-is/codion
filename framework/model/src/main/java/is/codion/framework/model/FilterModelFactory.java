/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnFilterModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * Responsible for creating {@link ColumnFilterModel} based on properties.
 */
public interface FilterModelFactory {

  /**
   * Creates a {@link ColumnFilterModel} for the given property
   * @param <T> the column value type
   * @param property the Property for which to create a {@link ColumnFilterModel}
   * @return a {@link ColumnFilterModel} for the given property, null if filtering should
   * not be allowed for this property
   */
  <T> ColumnFilterModel<Entity, Attribute<?>, T> createFilterModel(Property<T> property);
}
