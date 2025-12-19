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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import static java.util.Objects.requireNonNull;

/**
 * @param <T> the column type
 */
public interface ColumnConditionModel<T> extends AttributeConditionModel<T> {

	@Override
	Column<T> attribute();

	/**
	 * @param columnDefinition the column definition
	 * @return a new {@link ColumnConditionModel.Builder}
	 */
	static <T> ColumnConditionModel.Builder<T> builder(ColumnDefinition<T> columnDefinition) {
		return new DefaultColumnConditionModel.DefaultBuilder<>(requireNonNull(columnDefinition));
	}

	/**
	 * A builder for a {@link ColumnConditionModel}
	 */
	interface Builder<T> {

		/**
		 * @return a new {@link ColumnConditionModel} instance
		 */
		ColumnConditionModel<T> build();
	}
}
