/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Operator;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

final class MultiValueColumnCondition<T> extends AbstractColumnCondition<T> {

  private static final long serialVersionUID = 1;

  private static final int IN_CLAUSE_LIMIT = 100;//JDBC limit
  private static final String IN_PREFIX = " in (";
  private static final String NOT_IN_PREFIX = " not in (";

  MultiValueColumnCondition(Column<T> column, Collection<? extends T> values, Operator operator) {
    this(column, values, operator, true);
  }

  MultiValueColumnCondition(Column<T> column, Collection<? extends T> values, Operator operator,
                            boolean caseSensitive) {
    super(column, operator, values, caseSensitive);
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
    if (!(object instanceof MultiValueColumnCondition)) {
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
    boolean caseInsensitiveString = column().type().isString() && !caseSensitive();
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
