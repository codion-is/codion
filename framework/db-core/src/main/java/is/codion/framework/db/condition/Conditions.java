/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.db.Operator.EQUALS;
import static is.codion.common.db.Operator.NOT_EQUALS;
import static is.codion.framework.db.condition.NullCheck.IS_NULL;
import static is.codion.framework.domain.entity.Entities.getValues;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A factory class for {@link Condition} and it's descendants.
 */
public final class Conditions {

  private static final String NULL_CHECK = "nullCheck";

  private Conditions() {}

  /**
   * Creates a {@link Condition} instance specifying all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a condition specifying all entities of the given type
   */
  public static Condition condition(final EntityType<?> entityType) {
    return new EmptyCondition(entityType);
  }

  /**
   * Creates a {@link AttributeCondition} instance for specifying entities of the type identified by {@code entityType}
   * with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCheck the null check condition
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> AttributeCondition<T> condition(final Attribute<T> attribute, final NullCheck nullCheck) {
    return condition(attribute, requireNonNull(nullCheck, NULL_CHECK) == IS_NULL ? EQUALS : NOT_EQUALS, emptyList());
  }

  /**
   * Creates a {@link AttributeCondition} instance for selecting entities of the type identified by {@code entityType}
   * with a where condition based on {@code attribute}, the operators based on {@code operator} and {@code key}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a select condition based on the given key
   */
  public static AttributeCondition<Entity> condition(final Attribute<Entity> attribute, final Operator operator, final Key... keys) {
    return condition(attribute, operator, asList(requireNonNull(keys)));
  }

  /**
   * Creates a {@link AttributeCondition} instance for selecting the entities with the given keys,
   * assuming they are all of the same type.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the keys to base this condition on
   * @return a select condition based on the given value
   */
  public static AttributeCondition<Entity> condition(final Attribute<Entity> attribute, final Operator operator, final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultAttributeCondition<>(attribute, operator, keys);
  }

  /**
   * Creates a {@link AttributeCondition} instance for specifying entities  with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> AttributeCondition<T> condition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return condition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link AttributeCondition} instance for specifying entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a condition based on the given value
   */
  public static <T> AttributeCondition<T> condition(final Attribute<T> attribute, final Operator operator, final Collection<? extends T> values) {
    return new DefaultAttributeCondition<>(attribute, operator, requireNonNull(values));
  }

  /**
   * Creates a {@link Condition} based on the given key
   * @param key the key
   * @return a condition based on the given key
   */
  public static Condition condition(final Key key) {
    if (requireNonNull(key).isCompositeKey()) {
      return singleCompositeCondition(key.getAttributes(), EQUALS, key);
    }

    return condition(key.getAttribute(), EQUALS, (Object) key.get());
  }

  /**
   * Creates a {@link Condition} based on the given keys
   * @param keys the keys
   * @return a condition based on the given keys
   * @throws IllegalArgumentException in case {@code keys} is empty
   */
  public static Condition condition(final List<Key> keys) {
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("No keys specified for key condition");
    }
    final Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return compositeKeyCondition(keys, firstKey.getAttributes(), EQUALS);
    }

    return condition((Attribute<?>) firstKey.getAttribute(), EQUALS, getValues(keys));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public static SelectCondition selectCondition(final Key key) {
    return selectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static SelectCondition selectCondition(final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultSelectCondition(condition(keys));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return a select condition encompassing all entities of the given type
   */
  public static SelectCondition selectCondition(final EntityType<?> entityType) {
    return new DefaultSelectCondition(new EmptyCondition(entityType));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting entities of the type identified by {@code entityType},
   * using the given {@link Condition}
   * @param condition the column condition
   * @return a select condition based on the given column condition
   */
  public static SelectCondition selectCondition(final Condition condition) {
    return new DefaultSelectCondition(condition);
  }

  /**
   * Creates a {@link SelectCondition} instance for specifying entities with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCheck the null check condition
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> SelectCondition selectCondition(final Attribute<T> attribute, final NullCheck nullCheck) {
    return selectCondition(attribute, requireNonNull(nullCheck, NULL_CHECK) == IS_NULL ? EQUALS : NOT_EQUALS, emptyList());
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting entities of the type identified by {@code entityType}
   * with a where condition based on {@code attribute}, {@code operator} and {@code key}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the condition keys
   * @return a select condition based on the given key
   */
  public static SelectCondition selectCondition(final Attribute<Entity> attribute, final Operator operator, final Key... keys) {
    return selectCondition(attribute, operator, asList(requireNonNull(keys)));
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param attribute the attribute
   * @param operator the condition operator
   * @param keys the keys to base this condition on
   * @return a select condition based on the given value
   */
  public static SelectCondition selectCondition(final Attribute<Entity> attribute, final Operator operator, final List<Key> keys) {
    return selectCondition(condition(attribute, operator, keys));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> SelectCondition selectCondition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return selectCondition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link SelectCondition} instance for selecting entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the attribute type
   * @return a select condition based on the given value
   */
  public static <T> SelectCondition selectCondition(final Attribute<T> attribute, final Operator operator, final Collection<? extends T> values) {
    return selectCondition(condition(attribute, operator, values));
  }

  /**
   * Creates a {@link UpdateCondition} instance for updating all entities of the type identified by {@code entityType}
   * @param entityType the entityType
   * @return an update condition encompassing all entities of the given type
   */
  public static UpdateCondition updateCondition(final EntityType<?> entityType) {
    return new DefaultUpdateCondition(new EmptyCondition(entityType));
  }

  /**
   * Creates a {@link UpdateCondition} instance for specifying entities with a where condition based on a null check for {@code attribute}.
   * @param attribute the attribute
   * @param nullCheck the null check condition
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> UpdateCondition updateCondition(final Attribute<T> attribute, final NullCheck nullCheck) {
    return updateCondition(attribute, requireNonNull(nullCheck, NULL_CHECK) == IS_NULL ? EQUALS : NOT_EQUALS, emptyList());
  }

  /**
   * Creates a {@link UpdateCondition} instance for updating entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> UpdateCondition updateCondition(final Attribute<T> attribute, final Operator operator, final T... values) {
    return updateCondition(attribute, operator, asList(requireNonNull(values)));
  }

  /**
   * Creates a {@link UpdateCondition} instance for updating entities with a where condition based on {@code attribute},
   * the operators based on {@code operator} and {@code values}.
   * @param attribute the attribute
   * @param operator the condition operator
   * @param values the condition values
   * @param <T> the value type
   * @return an update condition based on the given value
   */
  public static <T> UpdateCondition updateCondition(final Attribute<T> attribute, final Operator operator, final Collection<T> values) {
    return updateCondition(condition(attribute, operator, values));
  }

  /**
   * Creates a {@link UpdateCondition} instance for updating entities of the type identified by {@code entityType},
   * using the given {@link Condition}
   * @param condition the column condition
   * @return an update condition based on the given column condition
   */
  public static UpdateCondition updateCondition(final Condition condition) {
    return new DefaultUpdateCondition(condition);
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction) {
    return new DefaultConditionCombination(conjunction);
  }

  /**
   * Initializes a new {@link Condition.Combination} instance
   * @param conjunction the Conjunction to use
   * @param conditions the Condition objects to be included in this set
   * @return a new {@link Condition.Combination} instance
   */
  public static Condition.Combination combination(final Conjunction conjunction, final Condition... conditions) {
    return new DefaultConditionCombination(conjunction, conditions);
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition of the given type
   * @param conditionType the condition type
   * @return a new Condition instance
   * @throws NullPointerException in case the condition type is null
   * @see EntityDefinition.Builder#conditionProvider(String, ConditionProvider)
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
   * @see EntityDefinition.Builder#conditionProvider(String, ConditionProvider)
   */
  public static CustomCondition customCondition(final ConditionType conditionType, final List<Attribute<?>> attributes,
                                                final List<Object> values) {
    return new DefaultCustomCondition(conditionType, attributes, values);
  }

  /**
   * Creates a {@link WhereCondition} for the given EntityCondition.
   * @param condition the condition
   * @param entityDefinition the definition
   * @return a WhereCondition
   */
  public static WhereCondition whereCondition(final Condition condition, final EntityDefinition entityDefinition) {
    return new DefaultWhereCondition(expand(condition, entityDefinition), entityDefinition);
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
    if (condition instanceof SelectCondition) {
      return expand(((SelectCondition) condition).getCondition(), definition);
    }
    if (condition instanceof UpdateCondition) {
      return expand(((UpdateCondition) condition).getCondition(), definition);
    }
    if (condition instanceof AttributeCondition) {
      final AttributeCondition<?> attributeCondition = (AttributeCondition<?>) condition;
      final Property<?> property = definition.getProperty(attributeCondition.getAttribute());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition(((ForeignKeyProperty) property).getColumnAttributes(),
                attributeCondition.getOperator(), (List<Key>) attributeCondition.getValues());
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
        conditionCombination.add(condition(attributes.get(i), IS_NULL));
      }
      else {
        conditionCombination.add(condition((Attribute<Object>) attributes.get(i), operator, value));
      }
    }

    return conditionCombination;
  }

  private static <T> Condition foreignKeyCondition(final List<Attribute<?>> foreignKeyColumnAttributes,
                                                   final Operator operator, final List<Key> keys) {
    if (foreignKeyColumnAttributes.size() > 1) {
      return compositeKeyCondition(keys, foreignKeyColumnAttributes, operator);
    }

    return condition((Attribute<T>) foreignKeyColumnAttributes.get(0), operator, getValues(keys));
  }

  private static void checkKeysParameter(final List<Key> keys) {
    requireNonNull(keys, "keys");
    if (keys.isEmpty()) {
      throw new IllegalArgumentException("One or more keys must be provided for condition");
    }
  }

  /**
   * An empty condition, with no values or attributes
   */
  private static final class EmptyCondition extends AbstractCondition {

    private static final long serialVersionUID = 1;

    EmptyCondition(final EntityType<?> entityType) {
      super(entityType);
    }

    @Override
    public List<?> getValues() {
      return emptyList();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return emptyList();
    }

    @Override
    public String getWhereClause(final EntityDefinition definition) {
      return "";
    }
  }
}
