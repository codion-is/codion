/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.Operator.EQUAL;
import static is.codion.framework.db.condition.DefaultForeignKeyConditionBuilder.compositeCondition;
import static is.codion.framework.db.condition.DefaultForeignKeyConditionBuilder.compositeKeyCondition;
import static is.codion.framework.domain.entity.Entity.getValues;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Specifies a query condition. A factory class for {@link Condition} and it's descendants.
 */
public interface Condition {

  /**
   * @return the entity type
   */
  EntityType entityType();

  /**
   * @return a list of the values this condition is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List<?> values();

  /**
   * @return a list of the attributes this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Attribute<?>> attributes();

  /**
   * Returns a new Combination instance, combining this condition with the given one, AND'ing together.
   * @param conditions the conditions to combine with this one
   * @return a new condition combination
   */
  Combination and(Condition... conditions);

  /**
   * Returns a new Combination instance, combining this condition with the given one, OR'ing together.
   * @param conditions the conditions to combine with this one
   * @return a new condition combination
   */
  Combination or(Condition... conditions);

  /**
   * Returns a string representing this condition, e.g. "column = ?" or "col1 is not null and col2 in (?, ?)".
   * @param definition the entity definition
   * @return a condition string
   */
  String toString(EntityDefinition definition);

  /**
   * @return a {@link SelectCondition.Builder} instance based on this condition
   */
  SelectCondition.Builder selectBuilder();

  /**
   * @return a {@link UpdateCondition.Builder} instance based on this condition
   */
  UpdateCondition.Builder updateBuilder();

  /**
   * An interface encapsulating a combination of Condition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Condition {

    /**
     * @return the Conditions comprising this Combination
     */
    Collection<Condition> conditions();

    /**
     * @return the conjunction
     */
    Conjunction conjunction();
  }

  /**
   * Creates a {@link Condition} instance specifying all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a condition specifying all entities of the given type
   */
  static Condition condition(EntityType entityType) {
    return new DefaultCondition(entityType);
  }

  /**
   * Creates a {@link Condition} based on the given key
   * @param key the key
   * @return a condition based on the given key
   */
  static Condition condition(Key key) {
    if (requireNonNull(key).attributes().size() > 1) {
      Map<Attribute<?>, Attribute<?>> attributeMap = key.attributes().stream()
              .collect(Collectors.toMap(Function.identity(), Function.identity()));
      Map<Attribute<?>, Object> valueMap = new HashMap<>();
      key.attributes().forEach(attribute -> valueMap.put(attribute, key.get(attribute)));

      return compositeCondition(attributeMap, EQUAL, valueMap);
    }

    return new MultiValueAttributeCondition<>(key.attribute(), singletonList(key.get()), EQUAL);
  }

  /**
   * Creates a {@link Condition} based on the given keys, assuming they are all based on the same attributes.
   * @param keys the keys
   * @return a condition based on the given keys
   * @throws IllegalArgumentException in case {@code keys} is empty
   */
  static Condition condition(Collection<Key> keys) {
    if (requireNonNull(keys).isEmpty()) {
      throw new IllegalArgumentException("No keys specified for key condition");
    }
    Key firstKey = (keys instanceof List) ? ((List<Key>) keys).get(0) : keys.iterator().next();
    if (firstKey.attributes().size() > 1) {
      Map<Attribute<?>, Attribute<?>> attributeMap = firstKey.attributes().stream()
              .collect(Collectors.toMap(Function.identity(), Function.identity()));
      List<Map<Attribute<?>, ?>> valueMaps = new ArrayList<>(keys.size());
      keys.forEach(key -> {//can't use stream and toMap() due to possible null values
        Map<Attribute<?>, Object> valueMap = new HashMap<>();
        key.attributes().forEach(attribute -> valueMap.put(attribute, key.get(attribute)));
        valueMaps.add(valueMap);
      });

      return compositeKeyCondition(attributeMap, EQUAL, valueMaps);
    }

    return new MultiValueAttributeCondition<>((Attribute<?>) firstKey.attribute(), getValues(keys), EQUAL);
  }

  /**
   * Creates a {@link ForeignKeyConditionBuilder} instance based on the given foreign key attribute.
   * @param foreignKey the foreign key to base the condition on
   * @return a {@link ForeignKeyConditionBuilder} instance
   */
  static ForeignKeyConditionBuilder where(ForeignKey foreignKey) {
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
  static <T> AttributeCondition.Builder<T> where(Attribute<T> attribute) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("Use Conditions.where(ForeignKey foreignKey) to create a foreign key based where condition");
    }

    return new DefaultAttributeConditionBuilder<>(attribute);
  }

  /**
   * Initializes a new {@link Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the conditions to combine
   * @return a new {@link Combination} instance
   */
  static Combination combination(Conjunction conjunction, Condition... conditions) {
    return new DefaultConditionCombination(conjunction, conditions);
  }

  /**
   * Initializes a new {@link Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the conditions to combine
   * @return a new {@link Combination} instance
   */
  static Combination combination(Conjunction conjunction, Collection<Condition> conditions) {
    return new DefaultConditionCombination(conjunction, conditions);
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition of the given type
   * @param conditionType the condition type
   * @return a new Condition instance
   * @throws NullPointerException in case the condition type is null
   * @see EntityDefinition.Builder#conditionProvider(ConditionType, ConditionProvider)
   */
  static CustomCondition customCondition(ConditionType conditionType) {
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
  static CustomCondition customCondition(ConditionType conditionType, List<Attribute<?>> attributes,
                                         List<Object> values) {
    return new DefaultCustomCondition(conditionType, attributes, values);
  }
}
