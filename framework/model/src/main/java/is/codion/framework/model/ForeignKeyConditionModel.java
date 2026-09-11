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

import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A foreign key condition model. The {@link Operator#EQUAL} operand component is based on either a {@link EntitySearchModel}
 * or a {@link EntityComboBoxModel}, the {@link Operator#IN} operand component on a {@link EntitySearchModel}.
 * <p>The operands are plain values, independent of these models, a component based on a model must be linked to the
 * operand it edits, {@link Operands#equal()} or {@link Operands#in()}. Entities of the referenced type updated or deleted
 * are replaced in, or removed from, the operands, via {@link PersistenceEvents}.
 * @see ForeignKeyConditionModel#builder()
 */
public interface ForeignKeyConditionModel extends AttributeConditionModel<Entity> {

	@Override
	ForeignKey attribute();

	/**
	 * Note that the selection of this search model is not the EQUAL operand, a component based on it
	 * must be linked to {@link Operands#equal()}.
	 * @return the {@link EntitySearchModel} to base the EQUAL operand component on, an empty {@link Optional} if the EQUAL operand is not based on a search model
	 */
	Optional<EntitySearchModel> equalSearchModel();

	/**
	 * Note that the selection of this combo box model is not the EQUAL operand, a component based on it
	 * must be linked to {@link Operands#equal()}.
	 * @return the {@link EntityComboBoxModel} to base the EQUAL operand component on, an empty {@link Optional} if the EQUAL operand is not based on a combo box model
	 */
	Optional<EntityComboBoxModel> equalComboBoxModel();

	/**
	 * Note that the selection of this search model is not the IN operand, a component based on it
	 * must be linked to {@link Operands#in()}.
	 * @return the {@link EntitySearchModel} to base the IN operand component on, an empty {@link Optional} if the IN operand is not available
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
		 * Note that the selection of this search model is not the EQUAL operand, a component
		 * based on it must be linked to {@link Operands#equal()}.
		 * @param equalSearchModel the search model to base the EQUAL operand component on
		 * @return this builder
		 */
		Builder equalSearchModel(EntitySearchModel equalSearchModel);

		/**
		 * Note that the selection of this combo box model is not the EQUAL operand, a component
		 * based on it must be linked to {@link Operands#equal()}.
		 * @param equalComboBoxModel the combo box model to base the EQUAL operand component on
		 * @return this builder
		 */
		Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel);

		/**
		 * Note that the selection of this search model is not the IN operand, a component
		 * based on it must be linked to {@link Operands#in()}.
		 * @param inSearchModel the search model to base the IN operand component on
		 * @return this builder
		 */
		Builder inSearchModel(EntitySearchModel inSearchModel);

		/**
		 * Sets the initial operator, the one {@link ForeignKeyConditionModel#clear()} reverts to. Defaults to the first of the available
		 * {@link ForeignKeyConditionModel#operators()} — {@link Operator#EQUAL} when an EQUAL operand is available, otherwise {@link Operator#IN}.
		 * @param operator the initial operator, must be one of the available operators
		 * @return this builder
		 */
		Builder operator(Operator operator);

		/**
		 * @param caption the caption to associate with the condition model, the foreign key caption for example
		 * @return this builder
		 * @see ForeignKeyConditionModel#caption()
		 */
		Builder caption(@Nullable String caption);

		/**
		 * @return a new {@link ForeignKeyConditionModel} instance
		 */
		ForeignKeyConditionModel build();
	}
}
