/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class SingleValueColumnCondition<T> extends AbstractColumnCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;
  private final boolean useLikeOperator;

  SingleValueColumnCondition(Column<T> column, T value, Operator operator) {
    this(column, value, operator, true, false);
  }

  SingleValueColumnCondition(Column<T> column, T value, Operator operator,
                             boolean caseSensitive, boolean useLikeOperator) {
    super(column, operator, value == null ? emptyList() : singletonList(value), caseSensitive);
    validateOperator(operator);
    this.value = value;
    this.useLikeOperator = useLikeOperator;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof SingleValueColumnCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    SingleValueColumnCondition<?> that = (SingleValueColumnCondition<?>) object;
    return Objects.equals(value, that.value)
            && useLikeOperator == that.useLikeOperator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value, useLikeOperator);
  }

  @Override
  protected String toString(String columnExpression) {
    switch (operator()) {
      case EQUAL:
      case NOT_EQUAL:
        return toStringEqual(columnExpression);
      case LESS_THAN:
        return columnExpression + " < ?";
      case LESS_THAN_OR_EQUAL:
        return columnExpression + " <= ?";
      case GREATER_THAN:
        return columnExpression + " > ?";
      case GREATER_THAN_OR_EQUAL:
        return columnExpression + " >= ?";
      default:
        throw new IllegalStateException("Unsupported single value operator: " + operator());
    }
  }

  private String toStringEqual(String columnExpression) {
    boolean notEqual = operator() == Operator.NOT_EQUAL;
    String identifier = columnExpression;
    if (value == null) {
      return identifier + (notEqual ? " IS NOT NULL" : " IS NULL");
    }

    boolean isString = column().type().isString();
    boolean caseInsensitiveString = isString && !caseSensitive();
    if (caseInsensitiveString) {
      identifier = "UPPER(" + identifier + ")";
    }
    String valuePlaceholder = caseInsensitiveString ? "UPPER(?)" : "?";
    if (isString && useLikeOperator) {
      return identifier + (notEqual ? " NOT LIKE " : " LIKE ") + valuePlaceholder;
    }

    return identifier + (notEqual ? " <> " : " = ") + valuePlaceholder;
  }

  protected void validateOperator(Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
        break;
      default:
        throw new IllegalArgumentException("Unsupported single value operator: " + operator);
    }
  }
}
