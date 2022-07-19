/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DualValueAttributeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DualValueAttributeCondition(Attribute<T> attribute, T lowerBound, T upperBound, Operator operator) {
    super(attribute, operator);
    this.lowerBound = requireNonNull(lowerBound, "A lower bound is required");
    this.upperBound = requireNonNull(upperBound, "An upper bound is required");
  }

  @Override
  public List<?> getValues() {
    return asList(lowerBound, upperBound);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return asList(getAttribute(), getAttribute());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DualValueAttributeCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DualValueAttributeCondition<?> that = (DualValueAttributeCondition<?>) object;
    return Objects.equals(lowerBound, that.lowerBound) &&
            Objects.equals(upperBound, that.upperBound);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), lowerBound, upperBound);
  }

  @Override
  protected String getConditionString(String columnExpression) {
    switch (getOperator()) {
      case BETWEEN:
        return "(" + columnExpression + " >= ? and " + columnExpression + " <= ?)";
      case NOT_BETWEEN:
        return "(" + columnExpression + " <= ? or " + columnExpression + " >= ?)";
      case BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " > ? and " + columnExpression + " < ?)";
      case NOT_BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " < ? or " + columnExpression + " > ?)";
      default:
        throw new IllegalStateException("Unsupported dual value operator: " + getOperator());
    }
  }
}
