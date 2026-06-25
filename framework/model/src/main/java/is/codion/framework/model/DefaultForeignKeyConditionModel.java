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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private final ForeignKey foreignKey;
	private final ConditionModel<Entity> condition;
	private final @Nullable EntitySearchModel equalSearchModel;
	private final @Nullable EntityComboBoxModel equalComboBoxModel;
	private final @Nullable EntitySearchModel inSearchModel;

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		foreignKey = builder.foreignKey;
		equalSearchModel = builder.equalSearchModel;
		equalComboBoxModel = builder.equalComboBoxModel;
		inSearchModel = builder.inSearchModel;
		List<Operator> operators = builder.operators();
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						.operators(operators)
						.operator(builder.operator == null ? operators.get(0) : builder.operator)
						.operands(new ForeignKeyOperands())
						.build();
	}

	@Override
	public ForeignKey attribute() {
		return foreignKey;
	}

	@Override
	public ConditionModel<Entity> condition() {
		return condition;
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public Optional<EntitySearchModel> equalSearchModel() {
		return Optional.ofNullable(equalSearchModel);
	}

	@Override
	public Optional<EntityComboBoxModel> equalComboBoxModel() {
		return Optional.ofNullable(equalComboBoxModel);
	}

	@Override
	public Optional<EntitySearchModel> inSearchModel() {
		return Optional.ofNullable(inSearchModel);
	}

	static final class DefaultBuilder implements Builder {

		private final ForeignKey foreignKey;

		private @Nullable EntitySearchModel equalSearchModel;
		private @Nullable EntityComboBoxModel equalComboBoxModel;
		private @Nullable EntitySearchModel inSearchModel;
		private @Nullable Operator operator;

		DefaultBuilder(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public Builder equalSearchModel(EntitySearchModel equalSearchModel) {
			this.equalSearchModel = requireNonNull(equalSearchModel);
			return this;
		}

		@Override
		public Builder equalComboBoxModel(EntityComboBoxModel equalComboBoxModel) {
			this.equalComboBoxModel = requireNonNull(equalComboBoxModel);
			return this;
		}

		@Override
		public Builder inSearchModel(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public Builder operator(Operator operator) {
			this.operator = requireNonNull(operator);
			return this;
		}

		@Override
		public ForeignKeyConditionModel build() {
			return new DefaultForeignKeyConditionModel(this);
		}

		private List<Operator> operators() {
			if (equalSearchModel != null && equalComboBoxModel != null) {
				throw new IllegalStateException("The EQUAL operand can not be based on both a search model and a combo box model");
			}
			boolean equal = equalSearchModel != null || equalComboBoxModel != null;
			if (!equal && inSearchModel == null) {
				throw new IllegalStateException("Neither EQUAL nor IN operator specified");
			}
			if (equal && inSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN);
			}
			if (equal) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL);
			}

			return asList(Operator.IN, Operator.NOT_IN);
		}
	}

	private final class ForeignKeyOperands implements Operands<Entity> {

		@Override
		public Value<Entity> equal() {
			if (equalComboBoxModel != null) {
				return equalComboBoxModel.selection().item();
			}
			if (equalSearchModel != null) {
				return equalSearchModel.selection().entity();
			}

			return Operands.super.equal();
		}

		@Override
		public ValueSet<Entity> in() {
			return inSearchModel == null ? Operands.super.in() : inSearchModel.selection().entities();
		}
	}
}
