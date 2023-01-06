/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A default FilterModelFactory implementation.
 */
public class DefaultFilterModelFactory implements FilterModelFactory {

  @Override
  public <T> ColumnConditionModel<Entity, Attribute<?>, T> createFilterModel(Property<T> property) {
    if (property.attribute().isEntity()) {
      return null;
    }

    DefaultColumnConditionModel<Entity, Attribute<?>, T> filterModel = new DefaultColumnConditionModel<>(
            property.attribute(), property.attribute().valueClass(), operators(property.attribute().valueClass()),
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

  private static List<Operator> operators(Class<?> columnClass) {
    if (columnClass.equals(Boolean.class)) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
