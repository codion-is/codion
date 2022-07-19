/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class SingleValueAttributeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  SingleValueAttributeCondition(Attribute<T> attribute, T value, Operator operator) {
    super(attribute, operator);
    this.value = requireNonNull(value, "A bound value is required");
  }

  @Override
  public List<?> getValues() {
    return singletonList(value);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return singletonList(getAttribute());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof SingleValueAttributeCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    SingleValueAttributeCondition<?> that = (SingleValueAttributeCondition<?>) object;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
  }

  @Override
  protected String getConditionString(String columnExpression) {
    switch (getOperator()) {
      case LESS_THAN:
        return columnExpression + " < ?";
      case LESS_THAN_OR_EQUAL:
        return columnExpression + " <= ?";
      case GREATER_THAN:
        return columnExpression + " > ?";
      case GREATER_THAN_OR_EQUAL:
        return columnExpression + " >= ?";
      default:
        throw new IllegalStateException("Unsupported single value operator: " + getOperator());
    }
  }
}
