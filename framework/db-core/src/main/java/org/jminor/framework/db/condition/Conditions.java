/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.Conjunction.OR;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.framework.domain.Entities.getValues;

/**
 * A factory class for {@link Condition}, {@link EntityCondition} and {@link EntitySelectCondition} instances
 */
public final class Conditions {

  private Conditions() {}

  /**
   * Creates a {@link EntityCondition} instance specifying the entity of the type identified by {@code key}
   * @param key the primary key
   * @return a condition specifying the entity with the given primary key
   */
  public static EntityCondition entityCondition(final Entity.Key key) {
    return entityCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntityCondition} instance specifying the entities of the type identified by {@code key},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a condition based on the given column condition
   */
  public static EntityCondition entityCondition(final String entityId, final Condition condition) {
    return new DefaultEntityCondition(entityId, condition);
  }

  /**
   * Creates a condition based on the given primary keys, it is assumed they are all of the same type
   * @param keys the primary keys
   * @return a condition specifying the entities having the given primary keys
   */
  public static EntityCondition entityCondition(final List<Entity.Key> keys) {
    final List<Entity.Key> keyList = checkKeysParameter(keys);
    return new DefaultEntityCondition(keyList.get(0).getEntityId(), createKeyCondition(keyList));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance specifying all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a condition specifying all entities of the given type
   */
  public static EntityCondition entityCondition(final String entityId) {
    return new DefaultEntityCondition(entityId);
  }

  /**
   * Creates a {@link EntityCondition} instance for specifying entities of the type identified by {@code entityId}
   * with a where condition based on the property identified by {@code propertyId}, the operators based on
   * {@code conditionType} and {@code value}. Note that {@code value} may be a single value, a Collection
   * of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a condition based on the given value
   */
  public static EntityCondition entityCondition(final String entityId, final String propertyId,
                                                final ConditionType conditionType, final Object value) {
    return new DefaultEntityCondition(entityId, propertyCondition(propertyId, conditionType, value));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entity with the given key
   * @param key the key
   * @return a select condition based on the given key
   */
  public static EntitySelectCondition entitySelectCondition(final Entity.Key key) {
    return entitySelectCondition(singletonList(requireNonNull(key, "key")));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting the entities with the given keys,
   * it is assumed they are all of the same type
   * @param keys the keys
   * @return a select condition based on the given keys
   */
  public static EntitySelectCondition entitySelectCondition(final List<Entity.Key> keys) {
    final List<Entity.Key> keyList = checkKeysParameter(keys);
    return new DefaultEntitySelectCondition(keyList.get(0).getEntityId(), createKeyCondition(keyList));
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting all entities of the type identified by {@code entityId}
   * @param entityId the entity ID
   * @return a select condition encompassing all entities of the given type
   */
  public static EntitySelectCondition entitySelectCondition(final String entityId) {
    return new DefaultEntitySelectCondition(entityId);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId},
   * using the given {@link Condition}
   * @param entityId the entity ID
   * @param condition the column condition
   * @return a select condition based on the given column condition
   */
  public static EntitySelectCondition entitySelectCondition(final String entityId, final Condition condition) {
    return new DefaultEntitySelectCondition(entityId, condition);
  }

  /**
   * Creates a {@link EntitySelectCondition} instance for selecting entities of the type identified by {@code entityId}
   * with a where condition based on the property identified by {@code propertyId}, the operators based on
   * {@code conditionType} and {@code value}. Note that {@code value} may be a single value, a Collection
   * of values or null.
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @param conditionType the condition type
   * @param value the condition value, can be a Collection of values
   * @return a select condition based on the given value
   */
  public static EntitySelectCondition entitySelectCondition(final String entityId, final String propertyId,
                                                            final ConditionType conditionType, final Object value) {
    return entitySelectCondition(entityId, propertyCondition(propertyId, conditionType, value));
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction) {
    return conditionSet(conjunction, Collections.emptyList());
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the Conjunction to use
   * @param conditions the Condition objects to be included in this set
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Condition... conditions) {
    return conditionSet(conjunction, asList(conditions));
  }

  /**
   * Initializes a new {@link Condition.Set} instance
   * @param conjunction the conjunction to use
   * @param condition the Condition objects to be included in this set
   * @return a new {@link Condition.Set} instance
   */
  public static Condition.Set conditionSet(final Conjunction conjunction, final Collection<Condition> condition) {
    return new DefaultConditionSet(conjunction, condition);
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition with the given id
   * @param conditionId the id of the condition
   * @return a new Condition instance
   * @throws NullPointerException in case the condition id
   * @see Entity.Definition.Builder#addConditionProvider(String, Entity.ConditionProvider)
   */
  public static CustomCondition customCondition(final String conditionId) {
    return customCondition(conditionId, emptyList(), emptyList());
  }

  /**
   * Creates a new {@link CustomCondition} based on the condition with the given id
   * @param conditionId the id of the condition
   * @param propertyIds the properties representing the values used by this condition, in the same order as their respective values
   * @param values the values used by this condition string
   * @return a new Condition instance
   * @throws NullPointerException in case any of the parameters are null
   * @see Entity.Definition.Builder#addConditionProvider(String, Entity.ConditionProvider)
   */
  public static CustomCondition customCondition(final String conditionId, final List<String> propertyIds, final List values) {
    return new DefaultCustomCondition(conditionId, propertyIds, values);
  }

  /**
   * Creates a {@link Condition} for the given property, with the operator specified by the {@code conditionType}
   * and {@code value}. Note that {@code value} may be a single value, a Collection of values or null.
   * @param propertyId the property
   * @param conditionType the search type
   * @param value the condition value, can be a Collection of values
   * @return a property condition based on the given value
   */
  public static PropertyCondition propertyCondition(final String propertyId, final ConditionType conditionType,
                                                    final Object value) {
    return new DefaultPropertyCondition(propertyId, conditionType, value);
  }

  /**
   * Creates a {@link WhereCondition} for the given EntityCondition.
   * @param entityCondition the condition
   * @param entityDefinition the definition
   * @return a WhereCondition
   */
  public static WhereCondition whereCondition(final EntityCondition entityCondition,
                                               final Entity.Definition entityDefinition) {
    return new DefaultWhereCondition(entityCondition,
            expand(entityCondition.getCondition(), entityDefinition), entityDefinition);
  }

  /**
   * Expands the given condition, that is, transforms property conditions based on foreign key
   * properties into column property conditions
   * @param condition the condition
   * @param definition the entity definition
   * @return an expanded Condition
   */
  public static Condition expand(final Condition condition, final Entity.Definition definition) {
    if (condition instanceof Condition.Set) {
      final Condition.Set conditionSet = (Condition.Set) condition;
      final ListIterator<Condition> conditionsIterator = conditionSet.getConditions().listIterator();
      while (conditionsIterator.hasNext()) {
        conditionsIterator.set(expand(conditionsIterator.next(), definition));
      }

      return condition;
    }
    if (condition instanceof PropertyCondition) {
      final PropertyCondition propertyCondition = (PropertyCondition) condition;
      final Property property = definition.getProperty(propertyCondition.getPropertyId());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition((ForeignKeyProperty) property, propertyCondition.getConditionType(),
                propertyCondition.getValues());
      }
    }

    return condition;
  }

  /**
   * Creates a composite condition from the given keys, referencing the given properties
   * @param keys the keys
   * @param properties the key properties
   * @param conditionType the condition type
   * @return a Condition referencing the given keys
   */
  public static Condition createCompositeKeyCondition(final List<Entity.Key> keys, final List<ColumnProperty> properties,
                                                      final ConditionType conditionType) {
    if (keys.size() == 1) {
      return createSingleCompositeCondition(properties, conditionType, keys.get(0));
    }

    return createMultipleCompositeCondition(properties, conditionType, keys);
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createKeyCondition(final List<Entity.Key> keys) {
    final Entity.Key firstKey = keys.get(0);
    if (firstKey.isCompositeKey()) {
      return createCompositeKeyCondition(keys, firstKey.getProperties(), LIKE);
    }

    return propertyCondition(firstKey.getFirstProperty().getPropertyId(), LIKE, getValues(keys));
  }

  /** Assumes {@code keys} is not empty. */
  private static Condition createMultipleCompositeCondition(final List<ColumnProperty> properties,
                                                            final ConditionType conditionType,
                                                            final List<Entity.Key> keys) {
    final Condition.Set conditionSet = conditionSet(OR);
    for (int i = 0; i < keys.size(); i++) {
      conditionSet.add(createSingleCompositeCondition(properties, conditionType, keys.get(i)));
    }

    return conditionSet;
  }

  private static Condition createSingleCompositeCondition(final List<ColumnProperty> properties,
                                                          final ConditionType conditionType,
                                                          final Entity.Key entityKey) {
    final Condition.Set conditionSet = conditionSet(AND);
    for (int i = 0; i < properties.size(); i++) {
      conditionSet.add(propertyCondition(properties.get(i).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.get(entityKey.getProperties().get(i))));
    }

    return conditionSet;
  }

  private static List<Entity.Key> checkKeysParameter(final List<Entity.Key> keys) {
    if (nullOrEmpty(keys)) {
      throw new IllegalArgumentException("Entity key condition requires at least one key");
    }

    return keys;
  }

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                               final ConditionType conditionType, final Collection values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return createCompositeKeyCondition(keys, foreignKeyProperty.getProperties(), conditionType);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
            getValues(keys));
  }

  private static List<Entity.Key> getKeys(final Object value) {
    final List<Entity.Key> keys = new ArrayList<>();
    if (value instanceof Collection) {
      if (((Collection) value).isEmpty()) {
        keys.add(null);
      }
      else {
        for (final Object object : (Collection) value) {
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
