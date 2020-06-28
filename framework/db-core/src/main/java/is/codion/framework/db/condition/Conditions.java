/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionProvider;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.util.List;
import java.util.ListIterator;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.db.Operator.EQUALS;
import static is.codion.framework.domain.entity.Entities.getValues;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

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
  public static Condition condition(final EntityType<?> entityType) {
    return new EmptyCondition(entityType);
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

    return new DefaultAttributeCondition<>(key.getAttribute(), EQUALS, requireNonNull(key.get()));
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

    return new DefaultAttributeCondition<>((Attribute<?>) firstKey.getAttribute(), EQUALS, requireNonNull(getValues(keys)));
  }

  /**
   * Creates a {@link AttributeCondition.Builder} instance based on the given attribute.
   * @param attribute the attribute to base the condition on
   * @param <T> the attribute type
   * @return a {@link AttributeCondition.Builder} instance
   */
  public static <T> AttributeCondition.Builder<T> condition(final Attribute<T> attribute) {
    return new DefaultAttributeCondition.DefaultBuilder<>(attribute);
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

  /**
   * Creates a {@link WhereCondition} for the given {@link Condition}.
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

  /* Assumes keys is not empty. */
  private static Condition multipleCompositeCondition(final List<Attribute<?>> properties, final Operator operator,
                                                      final List<Key> keys) {
    final Condition.Combination conditionCombination = new DefaultConditionCombination(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionCombination.add(singleCompositeCondition(properties, operator, keys.get(i)));
    }

    return conditionCombination;
  }

  private static Condition singleCompositeCondition(final List<Attribute<?>> attributes, final Operator operator,
                                                    final Key entityKey) {
    final Condition.Combination conditionCombination = new DefaultConditionCombination(AND);
    for (int i = 0; i < attributes.size(); i++) {
      final Object value = entityKey.get(entityKey.getAttributes().get(i));
      if (value == null) {
        conditionCombination.add(condition(attributes.get(i)).isNull());
      }
      else {
        conditionCombination.add(new DefaultAttributeCondition<>((Attribute<Object>) attributes.get(i),
                operator, requireNonNull(value)));
      }
    }

    return conditionCombination;
  }

  private static <T> Condition foreignKeyCondition(final List<Attribute<?>> foreignKeyColumnAttributes,
                                                   final Operator operator, final List<Key> keys) {
    if (foreignKeyColumnAttributes.size() > 1) {
      return compositeKeyCondition(keys, foreignKeyColumnAttributes, operator);
    }

    return new DefaultAttributeCondition<>((Attribute<T>) foreignKeyColumnAttributes.get(0), operator, requireNonNull(getValues(keys)));
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
