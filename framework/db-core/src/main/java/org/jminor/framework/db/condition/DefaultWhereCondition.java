/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;

import java.util.List;

final class DefaultWhereCondition implements WhereCondition {

  private final EntityDefinition entityDefinition;
  private final EntityCondition entityCondition;
  private final Condition condition;
  private final List values;
  private final List<ColumnProperty> columnProperties;

  DefaultWhereCondition(final EntityCondition entityCondition, final Condition expandedCondition,
                        final EntityDefinition entityDefinition) {
    this.entityDefinition = entityDefinition;
    this.entityCondition = entityCondition;
    this.condition = expandedCondition;
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
    if (condition instanceof PropertyCondition) {
      final PropertyCondition propertyCondition = (PropertyCondition) condition;

      return propertyCondition.getConditionString(entityDefinition.getColumnProperty(propertyCondition.getPropertyId()));
    }
    if (condition instanceof CustomCondition) {
      final CustomCondition customCondition = (CustomCondition) condition;

      return entityDefinition.getConditionProvider(customCondition.getConditionId())
              .getConditionString(customCondition.getPropertyIds(), customCondition.getValues());
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
