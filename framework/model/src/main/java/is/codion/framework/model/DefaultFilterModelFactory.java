/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

/**
 * A default FilterModelFactory implementation.
 */
public class DefaultFilterModelFactory implements FilterModelFactory {

  @Override
  public <T> ColumnFilterModel<Entity, Attribute<?>, T> createFilterModel(final Property<T> property) {
    if (property.getAttribute().isEntity()) {
      return null;
    }

    final DefaultColumnFilterModel<Entity, Attribute<?>, T> filterModel = new DefaultColumnFilterModel<>(
            property.getAttribute(), property.getAttribute().getTypeClass(),
            Property.WILDCARD_CHARACTER.get(), property.getFormat(), property.getDateTimePattern());
    filterModel.setComparableFunction(row -> {
      if (row.isNull(property.getAttribute())) {
        return null;
      }

      final Object value = row.get(property.getAttribute());
      if (value instanceof Entity) {
        return (Comparable<T>) value.toString();
      }

      return (Comparable<T>) value;
    });

    return filterModel;
  }
}
