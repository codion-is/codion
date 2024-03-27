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

import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * Provides condition strings for where clauses
 */
public interface ConditionProvider {

	/**
	 * Creates a query condition string for the given values
	 * @param columns the condition columns
	 * @param values the values
	 * @return a query condition string
	 */
	String toString(List<Column<?>> columns, List<?> values);
}
