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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static is.codion.common.Conjunction.AND;
import static is.codion.common.Conjunction.OR;
import static is.codion.common.db.Operator.EQUAL;
import static is.codion.common.db.Operator.NOT_EQUAL;
import static is.codion.framework.domain.entity.Entities.getValues;
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
      return singleCompositeCondition(attributeMap(key.getAttributes()), EQUAL, valueMap(key));
    }

    return new DefaultAttributeEqualCondition<>(key.getAttribute(), singletonList(key.get()));
  }

  /**
   * Creates a {@link Condition} based on the given keys, assuming they are all based on the same attributes.
   * @param keys the keys
   * @return a condition based on the given keys
   * @throws IllegalArgumentException in case {@code keys} is empty
   */
  public static Condition condition(final List<Key> keys) {
    if (requireNonNull(keys).isEmpty()) {
      throw new IllegalArgumentException("No keys specified for key condition");
    }
    final Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return compositeKeyCondition(attributeMap(firstKey.getAttributes()), EQUAL,
              keys.stream().map(Conditions::valueMap).collect(toList()));
    }

    return new DefaultAttributeEqualCondition<>((Attribute<?>) firstKey.getAttribute(), getValues(keys));
  }

  /**
   * Creates a {@link AttributeCondition.Builder} instance based on the given attribute.
   * @param attribute the attribute to base the condition on
   * @param <T> the attribute type
   * @return a {@link AttributeCondition.Builder} instance
   */
  public static <T> AttributeCondition.Builder<T> condition(final Attribute<T> attribute) {
    return new DefaultAttributeConditionBuilder<>(attribute);
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
   * attributes into column based attribute conditions
   * @param condition the condition
   * @param definition the entity definition
   * @return an expanded Condition
   */
  private static Condition expand(final Condition condition, final EntityDefinition definition) {
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
        return foreignKeyCondition((ForeignKeyProperty) property, attributeCondition.getOperator(),
                attributeCondition.getValues().stream().map(value -> valueMap((Entity) value)).collect(toList()));
      }
    }

    return condition;
  }

  private static Condition compositeKeyCondition(final Map<Attribute<?>, Attribute<?>> attributes, final Operator operator,
                                                 final List<Map<Attribute<?>, Object>> valueMaps) {
    if (valueMaps.size() == 1) {
      return singleCompositeCondition(attributes, operator, valueMaps.get(0));
    }

    return multipleCompositeCondition(attributes, operator, valueMaps);
  }

  /* Assumes keys is not empty. */
  private static Condition multipleCompositeCondition(final Map<Attribute<?>, Attribute<?>> attributes, final Operator operator,
                                                      final List<Map<Attribute<?>, Object>> valueMaps) {
    final Condition.Combination conditionCombination = combination(OR);
    valueMaps.forEach(valueMap -> conditionCombination.add(singleCompositeCondition(attributes, operator, valueMap)));

    return conditionCombination;
  }

  private static Condition singleCompositeCondition(final Map<Attribute<?>, Attribute<?>> attributes,
                                                    final Operator operator, final Map<Attribute<?>, Object> valueMap) {
    final Condition.Combination conditionCombination = combination(AND);
    attributes.forEach((conditionAttribute, valueAttribute) -> {
      final Object value = valueMap.get(valueAttribute);
      final AttributeCondition.Builder<Object> condition = condition((Attribute<Object>) conditionAttribute);
      if (operator == EQUAL) {
        conditionCombination.add(value == null ? condition.isNull() : condition.equalTo(value));
      }
      else if (operator == NOT_EQUAL) {
        conditionCombination.add(value == null ? condition.isNotNull() : condition.notEqualTo(value));
      }
      else {
        throw new IllegalArgumentException("Unsupported operator: " + operator);
      }
    });

    return conditionCombination;
  }

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                               final Operator operator, final List<Map<Attribute<?>, Object>> valueMaps) {
    if (foreignKeyProperty.getReferences().size() > 1) {
      return compositeKeyCondition(attributeMap(foreignKeyProperty), operator, valueMaps);
    }

    final ForeignKeyProperty.Reference<?> reference = foreignKeyProperty.getReferences().get(0);
    final List<Object> values = valueMaps.stream()
            .map(map -> map.get(reference.getReferencedAttribute())).collect(toList());
    if (operator == EQUAL) {
      return condition((Attribute<Object>) reference.getAttribute()).equalTo(values);
    }
    if (operator == NOT_EQUAL) {
      return condition((Attribute<Object>) reference.getAttribute()).notEqualTo(values);
    }

    throw new IllegalArgumentException("Unsupported operator: " + operator);
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(final Collection<Attribute<?>> attributes) {
    final Map<Attribute<?>, Attribute<?>> map = new HashMap<>(attributes.size());
    attributes.forEach(attribute -> map.put(attribute, attribute));

    return map;
  }

  private static Map<Attribute<?>, Attribute<?>> attributeMap(final ForeignKeyProperty foreignKeyProperty) {
    final Map<Attribute<?>, Attribute<?>> map = new HashMap<>(foreignKeyProperty.getReferences().size());
    foreignKeyProperty.getReferences().forEach(reference -> map.put(reference.getAttribute(), reference.getReferencedAttribute()));

    return map;
  }

  private static Map<Attribute<?>, Object> valueMap(final Entity entity) {
    final Map<Attribute<?>, Object> values = new HashMap<>();
    entity.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue()));

    return values;
  }

  private static Map<Attribute<?>, Object> valueMap(final Key key) {
    final Map<Attribute<?>, Object> values = new HashMap<>();
    key.getAttributes().forEach(attribute -> values.put(attribute, key.get(attribute)));

    return values;
  }

  /**
   * An empty condition, with no values or attributes
   */
  private static final class EmptyCondition extends AbstractCondition {

    private static final long serialVersionUID = 1;

    private EmptyCondition(final EntityType<?> entityType) {
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
