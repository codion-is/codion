/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

final class MultiValueAttributeCriteria<T> extends AbstractAttributeCriteria<T> {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  MultiValueAttributeCriteria(Attribute<T> attribute, Collection<? extends T> values, Operator operator) {
    this(attribute, values, operator, true);
  }

  MultiValueAttributeCriteria(Attribute<T> attribute, Collection<? extends T> values, Operator operator,
                              boolean caseSensitive) {
    super(attribute, operator, values, caseSensitive);
    for (Object value : values) {
      requireNonNull(value, "Condition values may not be null");
    }
    validateOperator(operator);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof MultiValueAttributeCriteria)) {
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
    boolean notEqual = operator() == Operator.NOT_EQUAL;
    String identifier = columnExpression;
    boolean caseInsensitiveString = attribute().isString() && !caseSensitive();
    if (caseInsensitiveString) {
      identifier = "upper(" + identifier + ")";
    }
    String valuePlaceholder = caseInsensitiveString ? "upper(?)" : "?";

    return createInList(identifier, valuePlaceholder, values().size(), notEqual);
  }

  private static String createInList(String columnIdentifier, String valuePlaceholder, int valueCount, boolean negated) {
    boolean exceedsLimit = valueCount > IN_CLAUSE_LIMIT;
    StringBuilder stringBuilder = new StringBuilder(exceedsLimit ? "(" : "").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
    int cnt = 1;
    for (int i = 0; i < valueCount; i++) {
      stringBuilder.append(valuePlaceholder);
      if (cnt++ == IN_CLAUSE_LIMIT && i < valueCount - 1) {
        stringBuilder.append(negated ? ") and " : ") or ").append(columnIdentifier).append(negated ? NOT_IN_PREFIX : IN_PREFIX);
        cnt = 1;
      }
      else if (i < valueCount - 1) {
        stringBuilder.append(", ");
      }
    }
    stringBuilder.append(")").append(exceedsLimit ? ")" : "");

    return stringBuilder.toString();
  }

  protected void validateOperator(Operator operator) {
    switch (operator) {
      case EQUAL:
      case NOT_EQUAL:
        break;
      default:
        throw new IllegalArgumentException("Unsupported multi value operator: " + operator);
    }
  }
}
