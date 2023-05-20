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
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ColumnConditionModel.Factory} implementation for creating filter models.
 */
public class EntityFilterModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

  private final EntityDefinition entityDefinition;

  /**
   * Instantiates a new {@link EntityFilterModelFactory}.
   * @param entityDefinition the entity definition
   */
  public EntityFilterModelFactory(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @return the underlying entity definition
   */
  public final EntityDefinition entityDefinition() {
    return entityDefinition;
  }

  @Override
  public Optional<ColumnConditionModel<? extends Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
    if (requireNonNull(attribute).isEntity()) {
      return Optional.empty();
    }
    if (!Comparable.class.isAssignableFrom(attribute.valueClass())) {
      return Optional.empty();
    }

    Property<?> property = entityDefinition.property(attribute);
    if (property.isHidden()) {
      return Optional.empty();
    }

    return Optional.ofNullable(ColumnConditionModel.builder(attribute, attribute.valueClass())
            .operators(operators(attribute.valueClass()))
            .format(property.format())
            .dateTimePattern(property.dateTimePattern())
            .build());
  }

  private static List<Operator> operators(Class<?> columnClass) {
    if (columnClass.equals(Boolean.class)) {
      return Collections.singletonList(Operator.EQUAL);
    }

    return Arrays.asList(Operator.values());
  }
}
