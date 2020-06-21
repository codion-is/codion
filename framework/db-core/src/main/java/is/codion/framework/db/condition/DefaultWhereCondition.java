/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

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
    return condition.getWhereClause(entityDefinition);
  }
}
