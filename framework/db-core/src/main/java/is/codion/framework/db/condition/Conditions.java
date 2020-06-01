/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.Util.nullOrEmpty;
import static is.codion.common.db.Operator.LIKE;
import static is.codion.framework.domain.entity.Entities.getValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link Condition} and it's descendants.
 */
public final class Conditions {

  private Conditions() {}

  /**
   * Creates a {@link EntityCondition} instance specifying the entity of the type identified by {@code key}
   * @param key the primary key
   * @return a condition specifying the entity with the given primary key
   */
  public static EntityCondition condition(final Entity.Key key) {
    return condition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entityId
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public static EntityCondition condition(final Entity.Identity entityId, final Condition condition) {
    return new DefaultEntityCondition(entityId, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public static EntityCondition condition(final List<Entity.Key> keys) {
    final List<Entity.Key> keyList = checkKeysParameter(keys);
    return new DefaultEntityCondition(keyList.get(0).getEntityId(), createKeyCondition(keyList));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance specifying all entities of the type identified by {@code entityId}
   * @param entityId the entityId
   * @return a condition specifying all entities of the given type
   */
  public static EntityCondition condition(final Entity.Identity entityId) {
    return new DefaultEntityCondition(entityId);
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities of the type identified by {@code entityId}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code value}.
   * Note that {@code value} may be a single value, a Collection of values or null.
   * @param entityId the entityId
   * @param attribute the attribute
   * @param operator the condition operator
   * @param value the condition value, can be a Collection of values
   * @return a condition based on the given value
   */
  public static EntityCondition condition(final Entity.Identity entityId, final Attribute<?> attribute,
                                          final Operator operator, final Object value) {
    return new DefaultEntityCondition(entityId, propertyCondition(attribute, operator, value));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition selectCondition(final Entity.Key key) {
    return selectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static EntitySelectCondition selectCondition(final List<Entity.Key> keys) {
    final List<Entity.Key> keyList = checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(keyList.get(0).getEntityId(), createKeyCondition(keyList));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting all entities of the type identified by {@code entityId}
   * @param entityId the entityId
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final Entity.Identity entityId) {
    return new DefaultEntitySelectCondition(entityId);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entityId
   * @param condition the column condition
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition selectCondition(final Entity.Identity entityId, final Condition condition) {
    return new DefaultEntitySelectCondition(entityId, condition);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code value}.
   * Note that {@code value} may be a single value, a Collection of values or null.
   * @param entityId the entityId
   * @param attribute the attribute
   * @param operator the condition operator
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public static EntitySelectCondition selectCondition(final Entity.Identity entityId, final Attribute<?> attribute,
                                                      final Operator operator, final Object value) {
    return selectCondition(entityId, propertyCondition(attribute, operator, value));
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating all entities of the type identified by {@code entityId}
   * @param entityId the entityId
   * @return an update condition encompassing all entities of the given type
   */
  public static EntityUpdateCondition updateCondition(final Entity.Identity entityId) {
    return new DefaultEntityUpdateCondition(entityId);
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating entities of the type identified by {@code entityId}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code value}.
   * Note that {@code value} may be a single value, a Collection of values or null.
   * @param entityId the entityId
   * @param attribute the attribute
   * @param operator the condition operator
   * @param value the condition value, can be a Collection of values
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> EntityUpdateCondition updateCondition(final Entity.Identity entityId, final Attribute<T> attribute,
                                                          final Operator operator, final T value) {
    return updateCondition(entityId, propertyCondition(attribute, operator, value));
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entityId
   * @param condition the column condition
   * @return an update condition based on the given column condition
   */
  public static EntityUpdateCondition updateCondition(final Entity.Identity entityId, final Condition condition) {
    return new DefaultEntityUpdateCondition(entityId, condition);
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction) {
    return combination(conjunction, Collections.emptyList());
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the Condition objects to be included in this set
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction, final Condition... conditions) {
    return combination(conjunction, asList(conditions));
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the conjunction to use
   * @param condition the Condition objects to be included in this set
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction, final Collection<Condition> condition) {
    return new DefaultConditionCombination(conjunction, condition);
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition with the given id
   * @param conditionId the id of the condition
   * @return a new Condition instance
   * @throws NullPointerException in case the condition id
   * @see EntityDefinition.Builder#conditionProvider(String, ConditionProvider)
   */
  public static CustomCondition customCondition(final String conditionId) {
    return customCondition(conditionId, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition with the given id
   * @param conditionId the id of the condition
   * @param attributes the attributes representing the values used by this condition, in the same order as their respective values
   * @param values the values used by this condition string
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   * @see EntityDefinition.Builder#conditionProvider(String, ConditionProvider)
   */
  public static CustomCondition customCondition(final String conditionId, final List<Attribute<?>> attributes, final List<Object> values) {
    return new DefaultCustomCondition(conditionId, attributes, values);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code operator}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static PropertyCondition propertyCondition(final Attribute<?> attribute, final Operator operator,
                                                    final Object value) {
    return new DefaultPropertyCondition(attribute, operator, value);
  }

  /**
   * Creates a {@link WhereCondition} for the given EntityCondition.
   * @param entityCondition the condition
   * @param entityDefinition the definition
   * @return a WhereCondition
   */
  public static WhereCondition whereCondition(final EntityCondition entityCondition, final EntityDefinition entityDefinition) {
    requireNonNull(entityCondition, "entityCondition");
    return new DefaultWhereCondition(expand(entityCondition.getCondition(), entityDefinition), entityDefinition);
  }

  /**
   * Expands the given condition, that is, transforms property conditions based on foreign key
   * properties into column property conditions
   * @param condition the condition
   * @param definition the entity definition
   * @return an expanded Condition
   */
  public static Condition expand(final Condition condition, final EntityDefinition definition) {
    requireNonNull(condition, "condition");
    requireNonNull(definition, "definition");
    if (condition instanceof Condition.Combination) {
      final Condition.Combination conditionCombination = (Condition.Combination) condition;
      final ListIterator<Condition> conditionsIterator = conditionCombination.getConditions().listIterator();
      while (conditionsIterator.hasNext()) {
        conditionsIterator.set(expand(conditionsIterator.next(), definition));
      }

      return condition;
    }
    if (condition instanceof PropertyCondition) {
      final PropertyCondition propertyCondition = (PropertyCondition) condition;
      final Property<?> property = definition.getProperty(propertyCondition.getAttribute());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition((ForeignKeyProperty) property, propertyCondition.getOperator(),
                propertyCondition.getValues());
      }
    }

    return condition;
  }

  private static Condition compositeKeyCondition(final List<Entity.Key> keys, final List<ColumnProperty<?>> properties,
                                                 final Operator operator) {
    if (keys.size() == 1) {
      return singleCompositeCondition(properties, operator, keys.get(0));
    }

    return multipleCompositeCondition(properties, operator, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return compositeKeyCondition(keys, firstKey.getProperties(), LIKE);
    }

    return propertyCondition(firstKey.getFirstProperty().getAttribute(), LIKE, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition multipleCompositeCondition(final List<ColumnProperty<?>> properties, final Operator operator,
                                                      final List<Entity.Key> keys) {
    final Condition.Combination conditionCombination = combination(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionCombination.add(singleCompositeCondition(properties, operator, keys.get(i)));
    }

    return conditionCombination;
  }

  private static Condition singleCompositeCondition(final List<ColumnProperty<?>> properties, final Operator operator,
                                                    final Entity.Key entityKey) {
    final Condition.Combination conditionCombination = combination(AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionCombination.add(propertyCondition(properties.get(i).getAttribute(), operator,
              entityKey == null ? null : entityKey.get(entityKey.getProperties().get(i).getAttribute())));
    }

    return conditionCombination;
  }

  private static List<Entity.Key> checkKeysParameter(final List<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }

    return keys;
  }

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                               final Operator operator, final Collection<Object> values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return compositeKeyCondition(keys, foreignKeyProperty.getColumnProperties(), operator);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return propertyCondition(foreignKeyProperty.getColumnProperties().get(0).getAttribute(), operator,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getColumnProperties().get(0).getAttribute(), operator,
            getValues(keys));
  }

  private static List<Entity.Key> getKeys(final Object value) {
    final List<Entity.Key> keys = new ArrayList<>();
    if (value instanceof Collection) {
      if (((Collection<Object>) value).isEmpty()) {
        keys.add(null);
      }
      else {
        for (final Object object : (Collection<Object>) value) {
          keys.add(getKey(object));
        }
      }
    }
    else {
      keys.add(getKey(value));
    }

    return keys;
  }

  private static Entity.Key getKey(final Object value) {
    if (value == null || value instanceof Entity.Key) {
      return (Entity.Key) value;
    }
    else if (value instanceof Entity) {
      return ((Entity) value).getKey();
    }

    throw new IllegalArgumentException("Foreign key condition uses only Entity or Entity.Key instances for values");
  }
}
