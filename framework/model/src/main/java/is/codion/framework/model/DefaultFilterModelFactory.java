/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A default FilterModelFactory implementation.
 */
public class DefaultFilterModelFactory implements FilterModelFactory {

  @Override
  public <T> ColumnConditionModel<Attribute<T>, T> createFilterModel(Property<T> property) {
    if (property.attribute().isEntity()) {
      return null;
    }
    if (!Comparable.class.isAssignableFrom(property.attribute().valueClass())) {
      return null;
    }

    return ColumnConditionModel.builder(property.attribute(), property.attribute().valueClass())
            .operators(operators(property.attribute().valueClass()))
            .format(property.format())
            .dateTimePattern(property.dateTimePattern())
            .build();
  }

  private static List<Operator> operators(Class<?> columnClass) {
    if (columnClass.equals(Boolean.class)) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
