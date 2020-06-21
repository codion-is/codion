/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.ColumnProperty;

import java.util.List;

final class DefaultWhereCondition implements WhereCondition {

  private final EntityDefinition entityDefinition;
  private final Condition condition;
  private final List<Object> values;
  private final List<ColumnProperty<?>> columnProperties;

  DefaultWhereCondition(final Condition expandedCondition, final EntityDefinition entityDefinition) {
    this.entityDefinition = entityDefinition;
    this.condition = expandedCondition;
    this.values = condition.getValues();
    this.columnProperties = entityDefinition.getColumnProperties(condition.getAttributes());
  }

  @Override
  public List<Object> getValues() {
    return values;
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties() {
    return columnProperties;
  }

  @Override
  public String getWhereClause() {
    return getWhereClause(condition);
  }

  private String getWhereClause(final Condition condition) {
    if (condition instanceof Condition.Combination) {
      final Condition.Combination conditionCombination = (Condition.Combination) condition;
      final List<Condition> conditions = conditionCombination.getConditions();
      if (conditions.size() == 1) {
        return getWhereClause(conditions.get(0));
      }

      final StringBuilder conditionString = new StringBuilder(conditions.size() > 1 ? "(" : "");
      final String conjunction = toString(conditionCombination.getConjunction());
      for (int i = 0; i < conditions.size(); i++) {
        conditionString.append(getWhereClause(conditions.get(i)));
        if (i < conditions.size() - 1) {
          conditionString.append(conjunction);
        }
      }

      return conditionString.append(conditions.size() > 1 ? ")" : "").toString();
    }
    if (condition instanceof AttributeCondition) {
      final AttributeCondition<Object> attributeCondition = (AttributeCondition<Object>) condition;

      return attributeCondition.getConditionString(entityDefinition.getColumnProperty(attributeCondition.getAttribute()));
    }
    if (condition instanceof CustomCondition) {
      final CustomCondition customCondition = (CustomCondition) condition;

      return entityDefinition.getConditionProvider(customCondition.getConditionId())
              .getConditionString(customCondition.getAttributes(), customCondition.getValues());
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
