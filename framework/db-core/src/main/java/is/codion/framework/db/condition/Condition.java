/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Criteria.Combination;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.CriteriaProvider;
import is.codion.framework.domain.entity.CriteriaType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.Operator.EQUAL;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Specifies a query condition, based on a {@link Criteria}.
 * A factory class for {@link Condition}, {@link Criteria} and it's descendants.
 * @see #all(EntityType)
 * @see #key(Key)
 * @see #keys(Collection)
 * @see #foreignKey(ForeignKey)
 * @see #attribute(Attribute)
 * @see #and(Criteria...)
 * @see #or(Criteria...)
 * @see #combination(Conjunction, Criteria...)
 * @see #combination(Conjunction, Criteria...)
 * @see #customCriteria(CriteriaType)
 * @see #customCriteria(CriteriaType, List, List)
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
   * @return the underlying Criteria instance
   */
  Criteria criteria();

  /**
   * @return a {@link SelectCondition.Builder} instance based on this condition
   */
  SelectCondition.Builder selectBuilder();

  /**
   * @return a {@link UpdateCondition.Builder} instance based on this condition
   */
  UpdateCondition.Builder updateBuilder();

  /**
   * @param criteria the criteria
   * @return a condition based on the given criteria
   */
  static Condition where(Criteria criteria) {
    return new DefaultCondition(criteria);
  }

  /**
   * Creates a {@link Condition} instance specifying all entities of type {@code entityType}
   * @param entityType the entityType
   * @return a condition specifying all entities of the given type
   */
  static Condition all(EntityType entityType) {
    return new DefaultCondition(Criteria.all(entityType));
  }

  /**
   * Returns a new {@link Combination} instance, combining the given criteria with AND.
   * @param criteria the criteria to combine
   * @return a new criteria combination
   */
  static Combination and(Criteria... criteria) {
    return new DefaultCriteriaCombination(Conjunction.AND, Arrays.asList(criteria));
  }

  /**
   * Returns a new {@link Combination} instance, combining the given criteria with OR.
   * @param criteria the criteria to combine
   * @return a new criteria combination
   */
  static Combination or(Criteria... criteria) {
    return new DefaultCriteriaCombination(Conjunction.OR, Arrays.asList(criteria));
  }

  /**
   * Creates a {@link Criteria} based on the given key
   * @param key the key
   * @return a criteria based on the given key
   */
  static Criteria key(Key key) {
    if (requireNonNull(key).attributes().size() > 1) {
      Map<Attribute<?>, Attribute<?>> attributeMap = key.attributes().stream()
              .collect(Collectors.toMap(Function.identity(), Function.identity()));
      Map<Attribute<?>, Object> valueMap = new HashMap<>();
      key.attributes().forEach(attribute -> valueMap.put(attribute, key.get(attribute)));

      return DefaultForeignKeyCriteriaBuilder.compositeEqualCriteria(attributeMap, EQUAL, valueMap);
    }

    return attribute(key.attribute()).equalTo(key.get());
  }

  /**
   * Creates a {@link Criteria} based on the given keys, assuming they are all based on the same attributes.
   * @param keys the keys
   * @return a criteria based on the given keys
   * @throws IllegalArgumentException in case {@code keys} is empty
   */
  static Criteria keys(Collection<Key> keys) {
    if (requireNonNull(keys).isEmpty()) {
      throw new IllegalArgumentException("No keys specified for key criteria");
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

      return DefaultForeignKeyCriteriaBuilder.compositeKeyCriteria(attributeMap, EQUAL, valueMaps);
    }

    return attribute((Attribute<?>) firstKey.attribute()).in(Entity.values(keys));
  }

  /**
   * Creates a {@link ForeignKeyCriteria.Builder} instance based on the given foreign key.
   * @param foreignKey the foreign key to base the criteria on
   * @return a {@link ForeignKeyCriteria.Builder} instance
   */
  static ForeignKeyCriteria.Builder foreignKey(ForeignKey foreignKey) {
    return new DefaultForeignKeyCriteriaBuilder(foreignKey);
  }

  /**
   * Creates a {@link AttributeCriteria.Builder} instance based on the given attribute.
   * @param attribute the attribute to base the criteria on
   * @param <T> the attribute type
   * @return a {@link AttributeCriteria.Builder} instance
   * @throws IllegalArgumentException in case {@code attribute} is a {@link ForeignKey}.
   * @see #foreignKey(ForeignKey)
   */
  static <T> AttributeCriteria.Builder<T> attribute(Attribute<T> attribute) {
    if (attribute instanceof ForeignKey) {
      throw new IllegalArgumentException("Use Condition.foreignKey(ForeignKey foreignKey) to create a foreign key based criteria");
    }

    return new DefaultAttributeCriteriaBuilder<>(attribute);
  }

  /**
   * Initializes a new {@link Combination} instance
   * @param conjunction the Conjunction to use
   * @param criteria the criteria to combine
   * @return a new {@link Combination} instance
   * @throws IllegalArgumentException in case {@code criteria} is empty
   */
  static Combination combination(Conjunction conjunction, Criteria... criteria) {
    return combination(conjunction, Arrays.asList(requireNonNull(criteria)));
  }

  /**
   * Initializes a new {@link Combination} instance
   * @param conjunction the Conjunction to use
   * @param criteria the criteria to combine
   * @return a new {@link Combination} instance
   * @throws IllegalArgumentException in case {@code criteria} is empty
   */
  static Combination combination(Conjunction conjunction, Collection<Criteria> criteria) {
    return new DefaultCriteriaCombination(conjunction, new ArrayList<>(requireNonNull(criteria)));
  }

  /**
   * Creates a new {@link CustomCriteria} based on the criteria of the given type
   * @param criteriaType the condition type
   * @return a new {@link CustomCriteria} instance
   * @throws NullPointerException in case the criteria type is null
   * @see EntityDefinition.Builder#criteriaProvider(CriteriaType, CriteriaProvider)
   */
  static CustomCriteria customCriteria(CriteriaType criteriaType) {
    return customCriteria(criteriaType, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link CustomCriteria} based on the criteria of the given type
   * @param criteriaType the criteria type
   * @param attributes the attributes representing the values used by this criteria, in the same order as their respective values
   * @param values the values used by this criteria string
   * @return a new {@link CustomCriteria} instance
   * @throws NullPointerException in case any of the parameters are null
   * @see EntityDefinition.Builder#criteriaProvider(CriteriaType, CriteriaProvider)
   */
  static CustomCriteria customCriteria(CriteriaType criteriaType, List<Attribute<?>> attributes,
                                       List<Object> values) {
    return new DefaultCustomCriteria(criteriaType, attributes, values);
  }
}
