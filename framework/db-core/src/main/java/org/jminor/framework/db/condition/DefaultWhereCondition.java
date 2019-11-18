/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;

import java.util.List;

final class DefaultWhereCondition implements WhereCondition {

  private final Entity.Definition entityDefinition;
  private final EntityCondition entityCondition;
  private final Condition condition;
  private final List values;
  private final List<ColumnProperty> columnProperties;

  DefaultWhereCondition(final EntityCondition entityCondition, final Entity.Definition entityDefinition) {
    this.entityDefinition = entityDefinition;
    this.entityCondition = entityCondition;
    this.condition = Conditions.expand(entityCondition.getCondition(), entityDefinition);
    this.values = condition.getValues();
    this.columnProperties = entityDefinition.getColumnProperties(condition.getPropertyIds());
  }

  @Override
  public EntityCondition getEntityCondition() {
    return entityCondition;
  }

  @Override
  public List getValues() {
    return values;
  }

  @Override
  public List<ColumnProperty> getColumnProperties() {
    return columnProperties;
  }

  @Override
  public String getWhereClause() {
    return getWhereClause(condition);
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
}
