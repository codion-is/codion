/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.db.Operator.EQUAL_TO;
import static is.codion.common.db.Operator.NOT_EQUAL_TO;
import static is.codion.framework.db.condition.NullCondition.IS_NULL;
import static is.codion.framework.domain.entity.Entities.getValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link Condition} and it's descendants.
 */
public final class Conditions {

  private static final String NULL_CONDITION = "nullCondition";

  private Conditions() {}

  /**
   * Creates a {@link EntityCondition} instance specifying the entity of the type identified by {@code key}
   * @param key the primary key
   * @return a condition specifying the entity with the given primary key
   */
  public static EntityCondition condition(final Key key) {
    return condition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entities of the type identified by {@code entityType},
   * using the given {@link Condition}
   * @param entityType the entityType
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public static EntityCondition condition(final EntityType<?> entityType, final Condition condition) {
    return new DefaultEntityCondition(entityType, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public static EntityCondition condition(final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntityCondition(keys.get(0).getEntityType(), createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance specifying all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a condition specifying all entities of the given type
   */
  public static EntityCondition condition(final EntityType<?> entityType) {
    return new DefaultEntityCondition(entityType);
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities of the type identified by {@code entityType}
   * with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCondition the null check condition
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> EntityCondition condition(final Attribute<T> attribute, final NullCondition nullCondition) {
    return condition(attribute, requireNonNull(nullCondition, NULL_CONDITION) == IS_NULL ? EQUAL_TO : NOT_EQUAL_TO, emptyList());
  }

  /**
   * Creates a {@link EntityCondition} instance for selecting entities of the type identified by {@code entityType}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code key}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a select condition based on the given key
   */
  public static EntityCondition condition(final Attribute<Entity> attribute, final Operator operator, final Key... keys) {
    return condition(attribute, operator, asList(requireNonNull(keys)));
  }

  /**
   * Creates a {@link EntityCondition} instance for selecting the entities with the given keys,
   * assuming they are all of the same type.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the keys to base this condition on
   * @return a select condition based on the given value
   */
  public static EntityCondition condition(final Attribute<Entity> attribute, final Operator operator, final List<Key> keys) {
    checkKeysParameter(keys);
    return condition(keys.get(0).getEntityType(), attributeCondition(attribute, operator, keys));
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities  with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> EntityCondition condition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return condition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> EntityCondition condition(final Attribute<T> attribute, final Operator operator, final Collection<? extends T> values) {
    return new DefaultEntityCondition(requireNonNull(attribute).getEntityType(), attributeCondition(attribute, operator, values));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition selectCondition(final Key key) {
    return selectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static EntitySelectCondition selectCondition(final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(keys.get(0).getEntityType(), createKeyCondition(keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition selectCondition(final EntityType<?> entityType) {
    return new DefaultEntitySelectCondition(entityType);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityType},
   * using the given {@link Condition}
   * @param entityType the entityType
   * @param condition the column condition
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition selectCondition(final EntityType<?> entityType, final Condition condition) {
    return new DefaultEntitySelectCondition(entityType, condition);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for specifying entities with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCondition the null check condition
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> EntitySelectCondition selectCondition(final Attribute<T> attribute, final NullCondition nullCondition) {
    return selectCondition(attribute, requireNonNull(nullCondition, NULL_CONDITION) == IS_NULL ? EQUAL_TO : NOT_EQUAL_TO, emptyList());
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityType}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code key}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition selectCondition(final Attribute<Entity> attribute, final Operator operator, final Key... keys) {
    return selectCondition(attribute, operator, asList(requireNonNull(keys)));
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the keys to base this condition on
   * @return a select condition based on the given value
   */
  public static EntitySelectCondition selectCondition(final Attribute<Entity> attribute, final Operator operator, final List<Key> keys) {
    checkKeysParameter(keys);
    return selectCondition(keys.get(0).getEntityType(), attributeCondition(attribute, operator, keys));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> EntitySelectCondition selectCondition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return selectCondition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> EntitySelectCondition selectCondition(final Attribute<T> attribute, final Operator operator, final Collection<? extends T> values) {
    return selectCondition(requireNonNull(attribute).getEntityType(), attributeCondition(attribute, operator, values));
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return an update condition encompassing all entities of the given type
   */
  public static EntityUpdateCondition updateCondition(final EntityType<?> entityType) {
    return new DefaultEntityUpdateCondition(entityType);
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for specifying entities with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCondition the null check condition
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> EntityUpdateCondition updateCondition(final Attribute<T> attribute, final NullCondition nullCondition) {
    return updateCondition(attribute, requireNonNull(nullCondition, NULL_CONDITION) == IS_NULL ? EQUAL_TO : NOT_EQUAL_TO, emptyList());
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> EntityUpdateCondition updateCondition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return updateCondition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> EntityUpdateCondition updateCondition(final Attribute<T> attribute, final Operator operator, final Collection<T> values) {
    return updateCondition(requireNonNull(attribute).getEntityType(), attributeCondition(attribute, operator, values));
  }

  /**
   * Creates a {@link EntityUpdateCondition} instance for updating entities of the type identified by {@code entityType},
   * using the given {@link Condition}
   * @param entityType the entityType
   * @param condition the column condition
   * @return an update condition based on the given column condition
   */
  public static EntityUpdateCondition updateCondition(final EntityType<?> entityType, final Condition condition) {
    return new DefaultEntityUpdateCondition(entityType, condition);
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
   * Creates a {@link Condition} instance with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCondition the null check condition
   * @param <T> the attribute type
   * @return a attribute condition based on the given value
   */
  public static <T> AttributeCondition<T> attributeCondition(final Attribute<T> attribute, final NullCondition nullCondition) {
    return attributeCondition(attribute, requireNonNull(nullCondition, NULL_CONDITION) == IS_NULL ? EQUAL_TO : NOT_EQUAL_TO, emptyList());
  }

  /**
   * Creates a {@link Condition} for the given attribute, with the operator specified by the {@code operator} and {@code key}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a attribute condition based on the given key
   */
  public static AttributeCondition<Entity> attributeCondition(final Attribute<Entity> attribute, final Operator operator,
                                                              final Key... keys) {
    return attributeCondition(attribute, operator, asList(requireNonNull(keys)));
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a attribute condition based on the given value
   */
  public static AttributeCondition<Entity> attributeCondition(final Attribute<Entity> attribute, final Operator operator,
                                                              final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultAttributeCondition<>(attribute, operator, keys);
  }

  /**
   * Creates a {@link Condition} for the given attribute, with the operator specified by the {@code operator}
   * and {@code value}. Note that {@code values} may be a single value or a Collection of values.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a attribute condition based on the given value
   */
  public static <T> AttributeCondition<T> attributeCondition(final Attribute<T> attribute, final Operator operator,
                                                             final T... values) {
    return attributeCondition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link Condition} for the given attribute, with the operator specified by the {@code operator}
   * and {@code value}. Note that {@code values} may be a single value or a Collection of values.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a attribute condition based on the given value
   */
  public static <T> AttributeCondition<T> attributeCondition(final Attribute<T> attribute, final Operator operator,
                                                             final Collection<? extends T> values) {
    return new DefaultAttributeCondition<>(attribute, operator, requireNonNull(values));
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
   * Expands the given condition, that is, transforms attribute conditions based on foreign key
   * attributes into column attribute conditions
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
    if (condition instanceof AttributeCondition) {
      final AttributeCondition<?> attributeCondition = (AttributeCondition<?>) condition;
      final Property<?> property = definition.getProperty(attributeCondition.getAttribute());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition(((ForeignKeyProperty) property).getColumnAttributes(),
                attributeCondition.getOperator(), attributeCondition.getValues());
      }
    }

    return condition;
  }

  private static Condition compositeKeyCondition(final List<Key> keys, final List<Attribute<?>> attributes,
                                                 final Operator operator) {
    if (keys.size() == 1) {
      return singleCompositeCondition(attributes, operator, keys.get(0));
    }

    return multipleCompositeCondition(attributes, operator, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static <T> Condition createKeyCondition(final List<Key> keys) {
    final Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return compositeKeyCondition(keys, firstKey.getAttributes(), EQUAL_TO);
    }

    return attributeCondition((Attribute<T>) firstKey.getAttribute(), EQUAL_TO, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition multipleCompositeCondition(final List<Attribute<?>> properties, final Operator operator,
                                                      final List<Key> keys) {
    final Condition.Combination conditionCombination = combination(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionCombination.add(singleCompositeCondition(properties, operator, keys.get(i)));
    }

    return conditionCombination;
  }

  private static Condition singleCompositeCondition(final List<Attribute<?>> attributes, final Operator operator,
                                                    final Key entityKey) {
    final Condition.Combination conditionCombination = combination(AND);
    for (int i = 0; i < attributes.size(); i++) {
      final Object value = entityKey.get(entityKey.getAttributes().get(i));
      if (value == null) {
        conditionCombination.add(attributeCondition(attributes.get(i), IS_NULL));
      }
      else {
        conditionCombination.add(attributeCondition((Attribute<Object>) attributes.get(i), operator, value));
      }
    }

    return conditionCombination;
  }

  private static <T> Condition foreignKeyCondition(final List<Attribute<?>> foreignKeyColumnAttributes,
                                                   final Operator operator, final Collection<Object> values) {
    final List<Key> keys = getKeys(values);
    if (foreignKeyColumnAttributes.size() > 1) {
      return compositeKeyCondition(keys, foreignKeyColumnAttributes, operator);
    }

    return attributeCondition((Attribute<T>) foreignKeyColumnAttributes.get(0), operator, getValues(keys));
  }

  private static void checkKeysParameter(final List<Key> keys) {
    requireNonNull(keys, "keys");
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("One or more keys must be provided for condition");
    }
  }

  private static List<Key> getKeys(final Object value) {
    final List<Key> keys = new ArrayList<>();
    if (value instanceof Collection) {
      for (final Object object : (Collection<Object>) value) {
        keys.add((Key) object);
      }
    }
    else {
      keys.add((Key) value);
    }

    return keys;
  }
}
