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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyConditionModel.DefaultBuilder;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A foreign key condition model. The {@link Operator#EQUAL} operand is based on either a {@link EntitySearchModel}
 * or a {@link EntityComboBoxModel}, the {@link Operator#IN} operand on a {@link EntitySearchModel}.
 * @see ForeignKeyConditionModel#builder()
 */
public interface ForeignKeyConditionModel extends AttributeConditionModel<Entity> {

	@Override
	ForeignKey attribute();

	/**
	 * @return the {@link EntitySearchModel} to use for the EQUAL operand, an empty {@link Optional} if the EQUAL operand is not based on a search model
	 */
	Optional<EntitySearchModel> equalSearchModel();

	/**
	 * @return the {@link EntityComboBoxModel} to use for the EQUAL operand, an empty {@link Optional} if the EQUAL operand is not based on a combo box model
	 */
	Optional<EntityComboBoxModel> equalComboBoxModel();

	/**
	 * @return the {@link EntitySearchModel} to use for the IN operand, an empty {@link Optional} if the IN operand is not available
	 */
	Optional<EntitySearchModel> inSearchModel();

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link Builder}
	 */
	static Builder builder(ForeignKey foreignKey) {
		return new DefaultBuilder(requireNonNull(foreignKey));
	}

	/**
	 * A builder for a {@link ForeignKeyConditionModel}.
	 * The EQUAL operand is based on either a {@link EntitySearchModel} or a {@link EntityComboBoxModel}, not both.
	 */
	interface Builder {

		/**
		 * Note that this search model is linked to the EQUAL operand, so no
		 * linking is required when constructing a UI component.
		 * @param equalSearchModel the search model to use for the EQUAL operand
		 * @return this builder
		 */
		Builder equalSearchModel(EntitySearchModel equalSearchModel);

		/**
		 * Note that this combo box model is linked to the EQUAL operand, so no
		 * linking is required when constructing a UI component.
		 * @param equalComboBoxModel the combo box model to use for the EQUAL operand
		 * @return this builder
		 */
		Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel);

		/**
		 * Note that this search model is linked to the IN operand, so no
		 * linking is required when constructing a UI component.
		 * @param inSearchModel the search model to use for the IN operand
		 * @return this builder
		 */
		Builder inSearchModel(EntitySearchModel inSearchModel);

		/**
		 * Sets the initial operator, the one {@link #clear()} reverts to. Defaults to the first of the available
		 * {@link #operators()} — {@link Operator#EQUAL} when an EQUAL operand is available, otherwise {@link Operator#IN}.
		 * @param operator the initial operator, must be one of the available operators
		 * @return this builder
		 */
		Builder operator(Operator operator);

		/**
		 * @return a new {@link ForeignKeyConditionModel} instance
		 */
		ForeignKeyConditionModel build();
	}
}
