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

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.DefaultForeignKeyConditionModel.DefaultBuilder;

/**
 * A default foreign key condition model using {@link EntitySearchModel} for
 * both the {@link Operator#EQUAL} and {@link Operator#IN} operands.
 * @see ForeignKeyConditionModel#builder()
 */
public interface ForeignKeyConditionModel extends ConditionModel<Entity> {

	/**
	 * @return a {@link EntitySearchModel} to use for the EQUAL operand
	 * @throws IllegalStateException in case no such model is available
	 */
	EntitySearchModel equalSearchModel();

	/**
	 * @return a {@link EntitySearchModel} to use for the IN operand
	 * @throws IllegalStateException in case no such model is available
	 */
	EntitySearchModel inSearchModel();

	/**
	 * @return a new {@link Builder}
	 */
	static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * A builder for a {@link ForeignKeyConditionModel}
	 */
	interface Builder {

		/**
		 * @param equalSearchModel the search model to use for the EQUAl operator
		 * @return this builder
		 */
		Builder equalSearchModel(EntitySearchModel equalSearchModel);

		/**
		 * @param inSearchModel the search model to use for the IN operator
		 * @return this builder
		 */
		Builder inSearchModel(EntitySearchModel inSearchModel);

		/**
		 * @return a new {@link ForeignKeyConditionModel} instance
		 */
		ForeignKeyConditionModel build();
	}
}
