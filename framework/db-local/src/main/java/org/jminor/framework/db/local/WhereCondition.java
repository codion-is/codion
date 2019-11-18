/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

final class WhereCondition {

  private final Entity.Definition entityDefinition;
  private final EntityCondition entityCondition;
  private final Condition condition;
  private final List values;
  private final List<ColumnProperty> columnProperties;

  WhereCondition(final EntityCondition entityCondition, final Entity.Definition entityDefinition) {
    this.entityDefinition = entityDefinition;
    this.entityCondition = entityCondition;
    this.condition = expand(entityCondition.getCondition(), entityDefinition);
    this.values = condition.getValues();
    this.columnProperties = entityDefinition.getColumnProperties(condition.getPropertyIds());
  }

  List getValues() {
    return values;
  }

  EntityCondition getEntityCondition() {
    return entityCondition;
  }

  List<ColumnProperty> getColumnProperties() {
    return columnProperties;
  }

  String getWhereClause() {
    return getWhereClause(condition);
  }

  static Condition expand(final Condition condition, final Entity.Definition definition) {
    if (condition instanceof Condition.Set) {
      final Condition.Set conditionSet = (Condition.Set) condition;
      final ListIterator<Condition> conditionsIterator = conditionSet.getConditions().listIterator();
      while (conditionsIterator.hasNext()) {
        conditionsIterator.set(expand(conditionsIterator.next(), definition));
      }

      return condition;
    }
    if (condition instanceof Condition.PropertyCondition) {
      final Condition.PropertyCondition propertyCondition = (Condition.PropertyCondition) condition;
      final Property property = definition.getProperty(propertyCondition.getPropertyId());
      if (property instanceof ForeignKeyProperty) {
        return foreignKeyCondition((ForeignKeyProperty) property, propertyCondition.getConditionType(),
                propertyCondition.getValues());
      }
    }

    return condition;
  }

  private String getWhereClause(final Condition condition) {
    if (condition instanceof Condition.Set) {
      final Condition.Set conditionSet = (Condition.Set) condition;
      final List<Condition> conditions = conditionSet.getConditions();
      final StringBuilder conditionString = new StringBuilder(conditions.size() > 1 ? "(" : "");
      final String conjunction = toString(conditionSet.getConjunction());
      for (int i = 0; i < conditions.size(); i++) {
        conditionString.append(getWhereClause(conditions.get(i)));
        if (i < conditions.size() - 1) {
          conditionString.append(conjunction);
        }
      }

      return conditionString.append(conditions.size() > 1 ? ")" : "").toString();
    }
    if (condition instanceof Condition.PropertyCondition) {
      final Condition.PropertyCondition propertyCondition = (Condition.PropertyCondition) condition;

      return propertyCondition.getConditionString(entityDefinition.getColumnProperty(propertyCondition.getPropertyId()));
    }
    if (condition instanceof Condition.CustomCondition) {
      final Condition.CustomCondition customCondition = (Condition.CustomCondition) condition;

      return entityDefinition.getConditionProvider(customCondition.getConditionId())
              .getConditionString(customCondition.getPropertyIds(), customCondition.getValues());
    }
    if (condition instanceof Condition.CustomStringCondition) {
      final Condition.CustomStringCondition customCondition = (Condition.CustomStringCondition) condition;

      return customCondition.getConditionString();
    }

    return "";
  }

  private static String toString(final Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }

  private static Condition foreignKeyCondition(final ForeignKeyProperty foreignKeyProperty,
                                                final ConditionType conditionType, final Collection values) {
    final List<Entity.Key> keys = getKeys(values);
    if (foreignKeyProperty.isCompositeKey()) {
      return Conditions.createCompositeKeyCondition(keys, foreignKeyProperty.getProperties(), conditionType);
    }

    if (keys.size() == 1) {
      final Entity.Key entityKey = keys.get(0);

      return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
              entityKey == null ? null : entityKey.getFirstValue());
    }

    return Conditions.propertyCondition(foreignKeyProperty.getProperties().get(0).getPropertyId(), conditionType,
            Entities.getValues(keys));
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
