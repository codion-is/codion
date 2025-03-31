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

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Defines a custom condition type.
 */
public interface ConditionType {

	/**
	 * @return the entity type
	 */
	EntityType entityType();

	/**
	 * @return the name
	 */
	String name();

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionProvider} associated with this {@link ConditionType}
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get();

	/**
	 * <p>Returns a {@link CustomCondition} based on the {@link ConditionProvider} associated with this {@link ConditionType}
	 * <p>This method assumes that the {@link ConditionProvider} is not based on any columns or has no need for them when creating the condition string.
	 * <p>Note that {@link ConditionProvider#toString(List, List)} will receive an empty column list.
	 * @param values the values used by this condition
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get(List<?> values);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionProvider} associated with this {@link ConditionType}
	 * @param column the column representing the value used by this condition
	 * @param value the value used by this condition string
	 * @param <T> the column type
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	<T> CustomCondition get(Column<T> column, @Nullable T value);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionProvider} associated with this {@link ConditionType}
	 * @param column the column representing the values used by this condition, assuming all the values are for the same column
	 * @param values the values used by this condition string
	 * @param <T> the column type
	 * @return a {@link CustomCondition} instance
	 * @see EntityDefinition#condition(ConditionType)
	 */
	<T> CustomCondition get(Column<T> column, List<T> values);

	/**
	 * Returns a {@link CustomCondition} based on the {@link ConditionProvider} associated with this {@link ConditionType}
	 * @param columns the columns representing the values used by this condition, in the same order as their respective values
	 * @param values the values used by this condition string in the same order as their respective columns
	 * @return a {@link CustomCondition} instance
	 * @throws IllegalArgumentException in case the number of columns does not match the number of values
	 * @see EntityDefinition#condition(ConditionType)
	 */
	CustomCondition get(List<Column<?>> columns, List<?> values);

	/**
	 * Instantiates a new {@link ConditionType} for the given entity type
	 * @param entityType the entityType
	 * @param name the name
	 * @return a new condition type
	 */
	static ConditionType conditionType(EntityType entityType, String name) {
		return new DefaultConditionType(entityType, name);
	}
}
