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

import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyConditionModel.DefaultBuilder;

import static java.util.Objects.requireNonNull;

/**
 * A default foreign key condition model using {@link EntitySearchModel} for
 * both the {@link Operator#EQUAL} and {@link Operator#IN} operands.
 * @see ForeignKeyConditionModel#builder()
 */
public interface ForeignKeyConditionModel extends AttributeConditionModel<Entity> {

	@Override
	ForeignKey attribute();

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
	 * @param foreignKey the foreign key
	 * @return a new {@link Builder}
	 */
	static Builder builder(ForeignKey foreignKey) {
		return new DefaultBuilder(requireNonNull(foreignKey));
	}

	/**
	 * A builder for a {@link ForeignKeyConditionModel}
	 */
	interface Builder {

		/**
		 * Note that this search model is linked to the EQUAL operand, so no
		 * linking is required when constructing a UI component.
		 * @param equalSearchModel the search model to use for the EQUAl operator
		 * @return this builder
		 */
		Builder equalSearchModel(EntitySearchModel equalSearchModel);

		/**
		 * Note that this search model is linked to the IN operand, so no
		 * linking is required when constructing a UI component.
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
