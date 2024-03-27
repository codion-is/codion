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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class SingleValueColumnCondition<T> extends AbstractColumnCondition<T> {

	private static final long serialVersionUID = 1;

	private static final String IS_NULL = " IS NULL";
	private static final String IS_NOT_NULL = " IS NOT NULL";
	private static final String EQUAL = " = ";
	private static final String NOT_EQUAL = " <> ";
	private static final String LIKE = " LIKE ";
	private static final String NOT_LIKE = " NOT LIKE ";
	private static final String LESS_THAN = " < ?";
	private static final String LESS_THAN_OR_EQUAL = " <= ?";
	private static final String GREATER_THAN = " > ?";
	private static final String GREATER_THAN_OR_EQUAL = " >= ?";
	private static final String PLACEHOLDER = "?";
	private static final String PLACEHOLDER_UPPER = "UPPER(?)";

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
	public String toString() {
		return "SingleValueColumnCondition{" +
						"column=" + column() +
						", operator=" + operator() +
						", value=" + value +
						", caseSensitive=" + caseSensitive() +
						", useLikeOperator=" + useLikeOperator + "}";
	}

	@Override
	protected String toString(String columnExpression) {
		switch (operator()) {
			case EQUAL:
				return toStringEqual(columnExpression);
			case NOT_EQUAL:
				return toStringNotEqual(columnExpression);
			case LESS_THAN:
				return columnExpression + LESS_THAN;
			case LESS_THAN_OR_EQUAL:
				return columnExpression + LESS_THAN_OR_EQUAL;
			case GREATER_THAN:
				return columnExpression + GREATER_THAN;
			case GREATER_THAN_OR_EQUAL:
				return columnExpression + GREATER_THAN_OR_EQUAL;
			default:
				throw new IllegalStateException("Unsupported single value operator: " + operator());
		}
	}

	private String toStringEqual(String columnExpression) {
		if (value == null) {
			return columnExpression + IS_NULL;
		}
		if (useLikeOperator) {
			return identifier(columnExpression) + LIKE + placeholder();
		}

		return identifier(columnExpression) + EQUAL + placeholder();
	}

	private String toStringNotEqual(String columnExpression) {
		if (value == null) {
			return columnExpression + IS_NOT_NULL;
		}
		if (useLikeOperator) {
			return identifier(columnExpression) + NOT_LIKE + placeholder();
		}

		return identifier(columnExpression) + NOT_EQUAL + placeholder();
	}

	private String identifier(String columnExpression) {
		return caseInsensitiveStringOrCharacter() ? "UPPER(" + columnExpression + ")" : columnExpression;
	}

	private String placeholder() {
		return caseInsensitiveStringOrCharacter() ? PLACEHOLDER_UPPER : PLACEHOLDER;
	}

	private boolean caseInsensitiveStringOrCharacter() {
		return !caseSensitive() && (column().type().isString() || column().type().isCharacter());
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
