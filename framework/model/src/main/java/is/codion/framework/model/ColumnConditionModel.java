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
package is.codion.framework.model;

import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * An {@link AttributeConditionModel} associated with a {@link Column}.
 * @param <T> the column type
 * @see EntityConditionModel.Builder#condition(Column, java.util.function.Consumer)
 */
public interface ColumnConditionModel<T> extends AttributeConditionModel<T> {

	@Override
	Column<T> attribute();

	/**
	 * A builder for a {@link ColumnConditionModel}
	 */
	interface Builder<T> {

		/**
		 * Sets the available operators, by default all operators, {@link Operator#EQUAL} only for a boolean column.
		 * @param operators the available operators
		 * @return this builder
		 * @throws IllegalArgumentException in case of an empty list
		 */
		Builder<T> operators(List<Operator> operators);

		/**
		 * Sets the initial operator, the one {@link ColumnConditionModel#clear()} reverts to.
		 * Defaults to {@link Operator#EQUAL}, or the first of the available {@link #operators(List)} when specified.
		 * @param operator the initial operator, must be one of the available operators
		 * @return this builder
		 */
		Builder<T> operator(Operator operator);

		/**
		 * @return a new {@link ColumnConditionModel} instance
		 * @throws IllegalArgumentException in case the operators don't contain the initial operator
		 */
		ColumnConditionModel<T> build();
	}
}
