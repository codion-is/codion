/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import static java.util.Objects.requireNonNull;
import static org.jminor.framework.domain.Entities.getValues;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final Condition.EmptyCondition EMPTY_CONDITION = new Condition.EmptyCondition();

  private String entityId;
  private Condition condition;
  private String cachedWhereClause;
  private boolean expandRequired = true;
  private transient boolean expanded;

  /**
   * Instantiates a new empty {@link DefaultEntityCondition}.
   * Using an empty condition means all underlying records should be selected
   * @param entityId the ID of the entity to select
   */
  DefaultEntityCondition(final String entityId) {
    this(entityId, null);
    this.expandRequired = false;
  }

  /**
   * Instantiates a new {@link EntityCondition}
   * @param entityId the ID of the entity to select
   * @param condition the Condition object
   */
  DefaultEntityCondition(final String entityId, final Condition condition) {
    this.entityId = requireNonNull(entityId, "entityId");
    this.condition = condition == null ? EMPTY_CONDITION : condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition getCondition(final Entity.Definition definition) {
    expandForeignKeyConditions(requireNonNull(definition, "definition"));

    return condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getWhereClause(final Entity.Definition definition) {
    if (cachedWhereClause == null) {
      cachedWhereClause = getCondition(definition).getConditionString(definition);
    }

    return cachedWhereClause;
  }

  private void expandForeignKeyConditions(final Entity.Definition definition) {
    if (expandRequired && !expanded) {
      condition = expandForeignKeyConditions(condition, definition);
      expanded = true;
    }
  }

  private static Condition expandForeignKeyConditions(final Condition condition, final Entity.Definition definition) {
    if (condition instanceof Condition.Set) {
      final Condition.Set conditionSet = (Condition.Set) condition;
      final ListIterator<Condition> conditionsIterator = conditionSet.getConditions().listIterator();
      while (conditionsIterator.hasNext()) {
        conditionsIterator.set(expandForeignKeyConditions(conditionsIterator.next(), definition));
      }
    }
    else if (condition instanceof Condition.PropertyCondition) {
      final Condition.PropertyCondition propertyCondition = (Condition.PropertyCondition) condition;
      final Property property = definition.getProperty(propertyCondition.getPropertyId());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition((ForeignKeyProperty) property, propertyCondition.getConditionType(), condition.getValues());
      }
    }

    return condition;
  }

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                               final ConditionType conditionType, final Collection values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return Conditions.createCompositeKeyCondition(foreignKeyProperty.getProperties(), conditionType, keys);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType, getValues(keys));
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

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(entityId);
    stream.writeObject(condition);
    stream.writeBoolean(expandRequired);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    entityId = (String) stream.readObject();
    condition = (Condition) stream.readObject();
    expandRequired = stream.readBoolean();
  }
}
