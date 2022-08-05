/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Text;
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
  public <T> ColumnFilterModel<Entity, Attribute<?>, T> createFilterModel(Property<T> property) {
    if (property.attribute().isEntity()) {
      return null;
    }

    DefaultColumnFilterModel<Entity, Attribute<?>, T> filterModel = new DefaultColumnFilterModel<>(
            property.attribute(), property.attribute().valueClass(),
            Text.WILDCARD_CHARACTER.get(), property.format(), property.dateTimePattern());
    filterModel.setComparableFunction(row -> {
      if (row.isNull(property.attribute())) {
        return null;
      }

      Object value = row.get(property.attribute());
      if (value instanceof Entity) {
        return (Comparable<T>) value.toString();
      }

      return (Comparable<T>) value;
    });

    return filterModel;
  }
}
