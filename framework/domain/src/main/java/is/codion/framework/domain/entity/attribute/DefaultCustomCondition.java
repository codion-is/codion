/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition extends AbstractCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final ConditionType conditionType;

  DefaultCustomCondition(ConditionType conditionType, List<Column<?>> columns, List<Object> values) {
    super(requireNonNull(conditionType).entityType(), columns, values);
    this.conditionType = conditionType;
  }

  @Override
  public ConditionType conditionType() {
    return conditionType;
  }

  @Override
  public String toString(EntityDefinition definition) {
    return requireNonNull(definition).conditionProvider(conditionType).toString(columns(), values());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultCustomCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultCustomCondition that = (DefaultCustomCondition) object;
    return conditionType.equals(that.conditionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), conditionType);
  }
}
