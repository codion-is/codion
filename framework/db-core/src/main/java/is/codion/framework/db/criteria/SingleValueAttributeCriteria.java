/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Column;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class SingleValueAttributeCriteria<T> extends AbstractAttributeCriteria<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  SingleValueAttributeCriteria(Column<T> attribute, T value, Operator operator) {
    this(attribute, value, operator, true);
  }

  SingleValueAttributeCriteria(Column<T> attribute, T value, Operator operator,
                               boolean caseSensitive) {
    super(attribute, operator, value == null ? emptyList() : singletonList(value), caseSensitive);
    validateOperator(operator);
    this.value = value;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof SingleValueAttributeCriteria)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    SingleValueAttributeCriteria<?> that = (SingleValueAttributeCriteria<?>) object;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), value);
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
      return identifier + (notEqual ? " is not null" : " is null");
    }

    boolean caseInsensitiveString = attribute().isString() && !caseSensitive();
    if (caseInsensitiveString) {
      identifier = "upper(" + identifier + ")";
    }
    String valuePlaceholder = caseInsensitiveString ? "upper(?)" : "?";
    if (attribute().isString() && containsWildcards((String) value)) {
      return identifier + (notEqual ? " not like " : " like ") + valuePlaceholder;
    }

    return identifier + (notEqual ? " <> " : " = ") + valuePlaceholder;
  }

  private static boolean containsWildcards(String value) {
    return value.contains("%") || value.contains("_");
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
