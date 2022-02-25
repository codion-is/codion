/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.NOT_EQUAL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyConditionBuilder implements ForeignKeyConditionBuilder {

  private static final String VALUES_PARAMETER = "values";

  private final ForeignKey foreignKey;

  DefaultForeignKeyConditionBuilder(final ForeignKey foreignKey) {
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public Condition equalTo(final Entity value) {
    if (value == null) {
      return isNull();
    }

    return equalTo(singletonList(value));
  }

  @Override
  public Condition equalTo(final Entity... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return equalTo(Arrays.asList(values));
  }

  @Override
  public Condition equalTo(final Collection<? extends Entity> values) {
    requireNonNull(values, VALUES_PARAMETER);

    List<Attribute<?>> attributes = foreignKey.getReferences().stream()
            .map(ForeignKey.Reference::getReferencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition notEqualTo(final Entity value) {
    if (value == null) {
      return isNotNull();
    }

    return notEqualTo(singletonList(value));
  }

  @Override
  public Condition notEqualTo(final Entity... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return notEqualTo(Arrays.asList(values));
  }

  @Override
  public Condition notEqualTo(final Collection<? extends Entity> values) {
    requireNonNull(values, VALUES_PARAMETER);

    List<Attribute<?>> attributes = foreignKey.getReferences().stream()
            .map(ForeignKey.Reference::getReferencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, NOT_EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition isNull() {
    return foreignKeyCondition(foreignKey, EQUAL, emptyList());
  }

  @Override
  public Condition isNotNull() {
    return foreignKeyCondition(foreignKey, NOT_EQUAL, emptyList());
  }

  private static Condition foreignKeyCondition(final ForeignKey foreignKey, final Operator operator,
                                               final List<Map<Attribute<?>, Object>> valueMaps) {
    if (foreignKey.getReferences().size() > 1) {
      return Conditions.compositeKeyCondition(attributeMap(foreignKey), operator, valueMaps);
    }

    ForeignKey.Reference<?> reference = foreignKey.getReferences().get(0);
    List<Object> values = valueMaps.stream()
            .map(map -> map.get(reference.getReferencedAttribute()))
            .collect(toList());
    if (operator == EQUAL) {
      return Conditions.where((Attribute<Object>) reference.getAttribute()).equalTo(values);
    }
    if (operator == NOT_EQUAL) {
      return Conditions.where((Attribute<Object>) reference.getAttribute()).notEqualTo(values);
    }

    throw new IllegalArgumentException("Unsupported operator: " + operator);
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(final ForeignKey foreignKeyProperty) {
    Map<Attribute<?>, Attribute<?>> map = new LinkedHashMap<>(foreignKeyProperty.getReferences().size());
    foreignKeyProperty.getReferences().forEach(reference -> map.put(reference.getAttribute(), reference.getReferencedAttribute()));

    return map;
  }

  private static Map<Attribute<?>, Object> valueMap(final Entity entity, final List<Attribute<?>> attributes) {
    Map<Attribute<?>, Object> values = new HashMap<>();
    attributes.forEach(attribute -> values.put(attribute, entity.get(attribute)));

    return values;
  }
}
