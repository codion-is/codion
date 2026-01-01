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

import static java.util.Objects.requireNonNull;

/**
 * Specifies a column template configuration.
 * {@snippet :
 * ColumnTemplate<Integer> REQUIRED_POSITIVE =
 *         column -> column
 *                 .nullable(false)
 *                 .minimum(0);
 *
 * Customer.AGE.as()
 *            .column(REQUIRED_POSITIVE)
 *            .caption("Age")
 *}
 * @param <T> the column type
 */
public interface ColumnTemplate<T> {

	/**
	 * Applies this column template to the given column definition builder
	 * @param column the column definition builder
	 * @return the column definition builder
	 */
	ColumnDefinition.Builder<T, ?> apply(ColumnDefinition.Builder<T, ?> column);

	/**
	 * Returns a composed template that applies this template followed by the given template.
	 * {@snippet :
	 * ColumnTemplate<String> REQUIRED = column -> column.nullable(false);
	 * ColumnTemplate<String> SEARCHABLE = column -> column.searchable(true);
	 *
	 * ColumnTemplate<String> REQUIRED_SEARCHABLE = REQUIRED.and(SEARCHABLE);
	 * // Equivalent to: column -> column.nullable(false).searchable(true)
	 *}
	 * @param template the template to apply after this template
	 * @return a composed template that applies this template followed by the given template
	 */
	default ColumnTemplate<T> and(ColumnTemplate<T> template) {
		requireNonNull(template);

		return (ColumnDefinition.Builder<T, ?> column) -> template.apply(apply(column));
	}
}
