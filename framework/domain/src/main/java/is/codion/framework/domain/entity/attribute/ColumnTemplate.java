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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

/**
 * Specifies a reusable column configuration.
 * {@snippet :
 * ColumnTemplate<Integer> REQUIRED_POSITIVE =
 *         column -> column.as()
 *                 .column()
 *                 .nullable(false)
 *                 .minimum(0);
 *
 * Customer.AGE.as(REQUIRED_POSITIVE)
 *         .caption("Age")
 *}
 * <p>A template configures the column from the ground up, so it is free to use any
 * {@link Column.ColumnDefiner} method, a subquery or primary key column is templated
 * just like a regular one.
 * {@snippet :
 * static ColumnTemplate<Integer> count(String subquery) {
 *     return column -> column.as()
 *             .subquery(subquery)
 *             .numberGrouping(true);
 * }
 *}
 * <p>Templates compose by applying the one being extended.
 * {@snippet :
 * ColumnTemplate<String> NAME =
 *         column -> column.as()
 *                 .column()
 *                 .maximumLength(50)
 *                 .searchable(true);
 *
 * ColumnTemplate<String> REQUIRED_NAME =
 *         column -> NAME.apply(column)
 *                 .nullable(false);
 *}
 * @param <T> the column type
 * @see Column#as(ColumnTemplate)
 */
@FunctionalInterface
public interface ColumnTemplate<T> {

	/**
	 * Applies this template to the given column
	 * @param column the column
	 * @return a {@link ColumnDefinition.Builder} for the given column
	 */
	ColumnDefinition.Builder<T, ?> apply(Column<T> column);
}
