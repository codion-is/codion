/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default ColumnConditionModel.Factory implementation.
 */
public class DefaultFilterModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

  private final EntityDefinition entityDefinition;

  public DefaultFilterModelFactory(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  @Override
  public ColumnConditionModel<? extends Attribute<?>, ?> createConditionModel(Attribute<?> attribute) {
    if (requireNonNull(attribute).isEntity()) {
      return null;
    }
    if (!Comparable.class.isAssignableFrom(attribute.valueClass())) {
      return null;
    }

    Property<?> property = entityDefinition.property(attribute);
    return ColumnConditionModel.builder(attribute, attribute.valueClass())
            .operators(operators(attribute.valueClass()))
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
