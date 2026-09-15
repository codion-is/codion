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

import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Operator;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.DefaultForeignKeyConditionModel.DefaultBuilder;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A foreign key condition model, the operands being entities of the referenced type.
 * <p>The operand components are based on the models provided by {@link #models()}, a combo box model and
 * a search model per operand, each created on first access. The operands are plain values, independent of
 * these models, a component based on one must be linked to the operand it edits, {@link Operands#equal()}
 * or {@link Operands#in()}. Entities of the referenced type updated or deleted are replaced in, or removed
 * from, the operands, via {@link PersistenceEvents}.
 * @see #builder()
 */
public interface ForeignKeyConditionModel extends AttributeConditionModel<Entity> {

	@Override
	ForeignKey attribute();

	/**
	 * @return the models the operand components are based on
	 */
	Models models();

	/**
	 * Provides the models the operand components are based on, each created on first access.
	 * <p>Note that the selection of these models is not the operand, a component based on one must be
	 * linked to the operand it edits. The EQUAL and IN operands have models of their own, since the
	 * components for both operands exist at the same time.
	 */
	interface Models {

		/**
		 * @return the models the EQUAL operand components are based on
		 */
		Operand equal();

		/**
		 * @return the models the IN operand components are based on
		 */
		Operand in();

		/**
		 * Restricts the referenced entities offered by the models, applied to each model, created or not.
		 * Note that a condition set directly on a model is left alone until this one is set.
		 * @return the {@link Value} controlling the condition restricting the referenced entities
		 * @see EntityComboBoxModel#condition()
		 * @see EntitySearchModel#condition()
		 */
		Value<Supplier<Condition>> condition();

		/**
		 * The models the components of an operand are based on, each created on first access.
		 */
		interface Operand {

			/**
			 * @return the combo box model, created on first access
			 * @throws IllegalStateException in case the model supplied is the one of the other operand
			 */
			EntityComboBoxModel comboBoxModel();

			/**
			 * @return the search model, created on first access
			 * @throws IllegalStateException in case the model supplied is the one of the other operand
			 */
			EntitySearchModel searchModel();
		}
	}

	/**
	 * @return a new {@link Builder.ForeignKeyStep}
	 */
	static Builder.ForeignKeyStep builder() {
		return DefaultBuilder.FOREIGN_KEY_STEP;
	}

	/**
	 * A builder for a {@link ForeignKeyConditionModel}.
	 */
	interface Builder {

		/**
		 * The first step in building a {@link ForeignKeyConditionModel}
		 */
		interface ForeignKeyStep {

			/**
			 * @param foreignKey the foreign key
			 * @return the {@link ConnectionStep}
			 */
			ConnectionStep foreignKey(ForeignKey foreignKey);
		}

		/**
		 * The second step in building a {@link ForeignKeyConditionModel}
		 */
		interface ConnectionStep {

			/**
			 * @param connection the connection, used by the default {@link Models}
			 * @return the {@link Builder}
			 */
			Builder connection(EntityConnection connection);
		}

		/**
		 * Supplies the combo box models, called once per operand, on first access, a separate instance per operand.
		 * Defaults to a combo box model of the referenced entities, including null.
		 * @param comboBoxModel supplies the combo box models
		 * @return this builder
		 * @see Models.Operand#comboBoxModel()
		 */
		Builder comboBoxModel(Supplier<EntityComboBoxModel> comboBoxModel);

		/**
		 * Supplies the search models, called once per operand, on first access, a separate instance per operand.
		 * Defaults to a search model of the referenced entities.
		 * @param searchModel supplies the search models
		 * @return this builder
		 * @see Models.Operand#searchModel()
		 */
		Builder searchModel(Supplier<EntitySearchModel> searchModel);

		/**
		 * Sets the available operators, a subset of {@link Operator#EQUAL}, {@link Operator#NOT_EQUAL},
		 * {@link Operator#IN} and {@link Operator#NOT_IN}, which is the default.
		 * @param operators the available operators
		 * @return this builder
		 * @throws IllegalArgumentException in case of an empty list or an operator not supported by a foreign key condition
		 */
		Builder operators(List<Operator> operators);

		/**
		 * Sets the initial operator, the one {@link ForeignKeyConditionModel#clear()} reverts to.
		 * Defaults to the first of the available {@link #operators(List)}.
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
		 * @throws IllegalArgumentException in case the initial operator is not one of the available operators
		 */
		ForeignKeyConditionModel build();
	}
}
