/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

final class DualValueAttributeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T lowerBound;
  private final T upperBound;

  DualValueAttributeCondition(Attribute<T> attribute, T lowerBound, T upperBound, Operator operator) {
    super(attribute, operator);
    validateOperator(operator);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  @Override
  public List<?> values() {
    return asList(lowerBound, upperBound);
  }

  @Override
  public List<Attribute<?>> attributes() {
    return asList(attribute(), attribute());
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
  protected String toString(String columnExpression) {
    switch (operator()) {
      case BETWEEN:
        return "(" + columnExpression + " >= ? and " + columnExpression + " <= ?)";
      case NOT_BETWEEN:
        return "(" + columnExpression + " < ? or " + columnExpression + " > ?)";
      case BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " > ? and " + columnExpression + " < ?)";
      case NOT_BETWEEN_EXCLUSIVE:
        return "(" + columnExpression + " <= ? or " + columnExpression + " >= ?)";
      default:
        throw new IllegalStateException("Unsupported dual value operator: " + operator());
    }
  }

  private static void validateOperator(Operator operator) {
    switch (operator) {
      case BETWEEN:
      case NOT_BETWEEN:
      case BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN_EXCLUSIVE:
        break;
      default:
        throw new IllegalStateException("Unsupported dual value operator: " + operator);
    }
  }
}
