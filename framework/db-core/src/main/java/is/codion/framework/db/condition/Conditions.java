/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.NOT_EQUAL;
import static is.codion.framework.domain.entity.Entity.getValues;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A factory class for {@link Condition} and it's descendants.
 */
public final class Conditions {

  private Conditions() {}

  /**
   * Creates a {@link Condition} instance specifying all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a condition specifying all entities of the given type
   */
  public static Condition condition(final EntityType entityType) {
    return new DefaultCondition(entityType);
  }

  /**
   * Creates a {@link Condition} based on the given key
   * @param key the key
   * @return a condition based on the given key
   */
  public static Condition condition(final Key key) {
    if (requireNonNull(key).getAttributes().size() > 1) {
      return compositeCondition(attributeMap(key.getAttributes()), EQUAL, valueMap(key));
    }

    return new DefaultAttributeEqualCondition<>(key.getAttribute(), singletonList(key.get()));
  }

  /**
   * Creates a {@link Condition} based on the given keys, assuming they are all based on the same attributes.
   * @param keys the keys
   * @return a condition based on the given keys
   * @throws IllegalArgumentException in case {@code keys} is empty
   */
  public static Condition condition(final Collection<Key> keys) {
    if (requireNonNull(keys).isEmpty()) {
      throw new IllegalArgumentException("No keys specified for key condition");
    }
    Key firstKey = (keys instanceof List) ? ((List<Key>) keys).get(0) : keys.iterator().next();
    if (firstKey.getAttributes().size() > 1) {
      return compositeKeyCondition(attributeMap(firstKey.getAttributes()), EQUAL, keys.stream()
              .map(Conditions::valueMap)
              .collect(toList()));
    }

    return new DefaultAttributeEqualCondition<>((Attribute<?>) firstKey.getAttribute(), getValues(keys));
  }

  /**
   * Creates a {@link ForeignKeyConditionBuilder} instance based on the given foreign key attribute.
   * @param foreignKey the foreign key to base the condition on
   * @return a {@link ForeignKeyConditionBuilder} instance
   */
  public static ForeignKeyConditionBuilder where(final ForeignKey foreignKey) {
    return new DefaultForeignKeyConditionBuilder(foreignKey);
  }

  /**
   * Creates a {@link AttributeCondition.Builder} instance based on the given attribute.
   * @param attribute the attribute to base the condition on
   * @param <T> the attribute type
   * @return a {@link AttributeCondition.Builder} instance
   * @throws IllegalArgumentException in case {@code attribute} is a {@link ForeignKey}.
   * @see #where(ForeignKey)
   */
  public static <T> AttributeCondition.Builder<T> where(final Attribute<T> attribute) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("Use Conditions.where(ForeignKey foreignKey) to create a foreign key based where condition");
    }

    return new DefaultAttributeConditionBuilder<>(attribute);
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the conditions to combine
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction, final Condition... conditions) {
    return new DefaultConditionCombination(conjunction, conditions);
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the conditions to combine
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction, final Collection<Condition> conditions) {
    return new DefaultConditionCombination(conjunction, conditions);
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition of the given type
   * @param conditionType the condition type
   * @return a new Condition instance
   * @throws NullPointerException in case the condition type is null
   * @see EntityDefinition.Builder#conditionProvider(ConditionType, ConditionProvider)
   */
  public static CustomCondition customCondition(final ConditionType conditionType) {
    return customCondition(conditionType, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition of the given type
   * @param conditionType the condition type
   * @param attributes the attributes representing the values used by this condition, in the same order as their respective values
   * @param values the values used by this condition string
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   * @see EntityDefinition.Builder#conditionProvider(ConditionType, ConditionProvider)
   */
  public static CustomCondition customCondition(final ConditionType conditionType, final List<Attribute<?>> attributes,
                                                final List<Object> values) {
    return new DefaultCustomCondition(conditionType, attributes, values);
  }

  static Condition compositeKeyCondition(final Map<Attribute<?>, Attribute<?>> attributes, final Operator operator,
                                         final List<Map<Attribute<?>, Object>> valueMaps) {
    if (valueMaps.size() == 1) {
      return compositeCondition(attributes, operator, valueMaps.get(0));
    }

    return combination(OR, valueMaps.stream()
            .map(valueMap -> compositeCondition(attributes, operator, valueMap))
            .collect(toList()));
  }

  private static Condition compositeCondition(final Map<Attribute<?>, Attribute<?>> attributes,
                                              final Operator operator, final Map<Attribute<?>, Object> valueMap) {
    return combination(AND, attributes.entrySet().stream()
            .map(entry -> condition(entry.getKey(), operator, valueMap.get(entry.getValue())))
            .collect(toList()));
  }

  private static Condition condition(final Attribute<?> conditionAttribute, final Operator operator, final Object value) {
    AttributeCondition.Builder<Object> condition = where((Attribute<Object>) conditionAttribute);
    if (operator == EQUAL) {
      return condition.equalTo(value);
    }
    else if (operator == NOT_EQUAL) {
      return condition.notEqualTo(value);
    }
    else {
      throw new IllegalArgumentException("Unsupported operator: " + operator);
    }
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(final Collection<Attribute<?>> attributes) {
    Map<Attribute<?>, Attribute<?>> map = new LinkedHashMap<>(attributes.size());
    attributes.forEach(attribute -> map.put(attribute, attribute));

    return map;
  }

  private static Map<Attribute<?>, Object> valueMap(final Key key) {
    Map<Attribute<?>, Object> values = new HashMap<>();
    key.getAttributes().forEach(attribute -> values.put(attribute, key.get(attribute)));

    return values;
  }
}
