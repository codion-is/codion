/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DualValueColumnCondition<T> extends AbstractColumnCondition<T> {

  private static final long serialVersionUID = 1;

  DualValueColumnCondition(Column<T> column, T lowerBound, T upperBound, Operator operator) {
    super(column, operator, asList(requireNonNull(lowerBound, "lowerBound"),
            requireNonNull(upperBound, "upperBound")), true);
    validateOperator(operator);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DualValueColumnCondition)) {
      return false;
    }

    return super.equals(object);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  protected String toString(String columnExpression) {
    switch (operator()) {
      case BETWEEN:
        return "(" + columnExpression + " >= ? AND " + columnExpression + " <= ?)";
      case NOT_BETWEEN:
        return "(" + columnExpression + " < ? OR " + columnExpression + " > ?)";
      case BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " > ? AND " + columnExpression + " < ?)";
      case NOT_BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " <= ? OR " + columnExpression + " >= ?)";
      default:
        throw new IllegalStateException("Unsupported dual value operator: " + operator());
    }
  }

  protected void validateOperator(Operator operator) {
    switch (operator) {
      case BETWEEN:
      case NOT_BETWEEN:
      case BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN_EXCLUSIVE:
        break;
      default:
        throw new IllegalArgumentException("Unsupported dual value operator: " + operator);
    }
  }
}
