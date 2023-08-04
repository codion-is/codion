/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.NOT_EQUAL;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyConditionBuilder implements ForeignKeyConditionBuilder {

  private static final String VALUES_PARAMETER = "values";

  private final ForeignKey foreignKey;

  DefaultForeignKeyConditionBuilder(ForeignKey foreignKey) {
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public Condition equalTo(Entity value) {
    if (value == null) {
      return isNull();
    }

    return in(singletonList(value));
  }

  @Override
  public Condition notEqualTo(Entity value) {
    if (value == null) {
      return isNotNull();
    }

    return notIn(singletonList(value));
  }

  @Override
  public Condition in(Entity... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return in(Arrays.asList(values));
  }

  @Override
  public Condition notIn(Entity... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return notIn(Arrays.asList(values));
  }

  @Override
  public Condition in(Collection<? extends Entity> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return isNull();
    }

    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(ForeignKey.Reference::referencedAttribute)
            .collect(toList());

    return foreignKeyCondition(foreignKey, EQUAL, values.stream()
            .map(entity -> valueMap(entity, attributes))
            .collect(toList()));
  }

  @Override
  public Condition notIn(Collection<? extends Entity> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return isNotNull();
    }

    List<Attribute<?>> attributes = foreignKey.references().stream()
            .map(ForeignKey.Reference::referencedAttribute)
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

  static Condition compositeKeyCondition(Map<Attribute<?>, Attribute<?>> attributes, Operator operator,
                                         List<Map<Attribute<?>, ?>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeCondition(attributes, operator, valueMaps.get(0));
    }

    return Condition.combination(OR, valueMaps.stream()
            .map(valueMap -> compositeCondition(attributes, operator, valueMap))
            .collect(toList()));
  }

  static Condition compositeCondition(Map<Attribute<?>, Attribute<?>> attributes,
                                      Operator operator, Map<Attribute<?>, ?> valueMap) {
    return Condition.combination(AND, attributes.entrySet().stream()
            .map(entry -> condition(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Condition foreignKeyCondition(ForeignKey foreignKey, Operator operator,
                                               List<Map<Attribute<?>, ?>> valueMaps) {
    if (foreignKey.references().size() > 1) {
      return compositeKeyCondition(attributeMap(foreignKey), operator, valueMaps);
    }

    ForeignKey.Reference<?> reference = foreignKey.references().get(0);
    List<Object> values = valueMaps.stream()
            .map(map -> map.get(reference.referencedAttribute()))
            .collect(toList());
    if (operator == EQUAL) {
      return Condition.where((Attribute<Object>) reference.attribute()).in(values);
    }
    if (operator == NOT_EQUAL) {
      return Condition.where((Attribute<Object>) reference.attribute()).notIn(values);
    }

    throw new IllegalArgumentException("Unsupported operator: " + operator);
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(ForeignKey foreignKeyProperty) {
    Map<Attribute<?>, Attribute<?>> map = new LinkedHashMap<>(foreignKeyProperty.references().size());
    foreignKeyProperty.references().forEach(reference -> map.put(reference.attribute(), reference.referencedAttribute()));

    return map;
  }

  private static Map<Attribute<?>, Object> valueMap(Entity entity, List<Attribute<?>> attributes) {
    Map<Attribute<?>, Object> values = new HashMap<>();
    attributes.forEach(attribute -> values.put(attribute, entity.get(attribute)));

    return values;
  }

  private static Condition condition(Attribute<?> conditionAttribute, Operator operator, Object value) {
    AttributeCondition.Builder<Object> condition = Condition.where((Attribute<Object>) conditionAttribute);
    if (operator == EQUAL) {
      return condition.equalTo(value);
    }
    if (operator == NOT_EQUAL) {
      return condition.notEqualTo(value);
    }

    throw new IllegalArgumentException("Unsupported operator: " + operator);
  }
}
