/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class SingleValueAttributeCondition<T> extends AbstractAttributeCondition<T> {

  private static final long serialVersionUID = 1;

  private final T value;

  SingleValueAttributeCondition(Attribute<T> attribute, T value, Operator operator) {
    this(attribute, value, operator, true);
  }

  SingleValueAttributeCondition(Attribute<T> attribute, T value, Operator operator,
                                boolean caseSensitive) {
    super(attribute, operator, caseSensitive);
    validateOperator(operator);
    this.value = value;
  }

  @Override
  public List<?> values() {
    if (value == null) {
      return emptyList();
    }

    return singletonList(value);
  }

  @Override
  public List<Attribute<?>> attributes() {
    if (value == null) {
      return emptyList();
    }

    return singletonList(attribute());
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

  private static void validateOperator(Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL:
        break;
      default:
        throw new IllegalStateException("Unsupported single value operator: " + operator);
    }
  }
}
