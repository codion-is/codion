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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import java.io.Serial;

import static java.util.Arrays.asList;

final class DualValueColumnCondition<T> extends AbstractColumnCondition<T> {

	@Serial
	private static final long serialVersionUID = 1;

	DualValueColumnCondition(Column<T> column, T lower, T upper, Operator operator) {
		super(column, operator, asList(lower, upper), true);
		if (lower == null || upper == null) {
			throw new IllegalArgumentException("Operator " + operator + " does not support null bound values");
		}
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
	protected String string(String columnExpression) {
		switch (operator()) {
			case BETWEEN:
				return "(" + columnExpression + " >= ? AND " + columnExpression + " <= ?)";
			case NOT_BETWEEN:
				return "(" + columnExpression + " <= ? OR " + columnExpression + " >= ?)";
			case BETWEEN_EXCLUSIVE:
				return "(" + columnExpression + " > ? AND " + columnExpression + " < ?)";
			case NOT_BETWEEN_EXCLUSIVE:
				return "(" + columnExpression + " < ? OR " + columnExpression + " > ?)";
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
				throw new IllegalArgumentException("Unsupported dual value operator: " + operator);
		}
	}
}
