/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * A default FilterModelFactory implementation.
 */
public class DefaultFilterModelFactory implements FilterModelFactory {

  @Override
  public <T> ColumnConditionModel<Entity, Attribute<?>, T> createFilterModel(final Property<T> property) {
    return new DefaultPropertyFilterModel<>(property);
  }
}
