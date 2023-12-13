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
  public String toString() {
    return "DualValueColumnCondition{" +
            "column=" + column() +
            ", operator=" + operator() +
            ", values=" + values() +
            ", caseSensitive=" + caseSensitive() + "}";
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
