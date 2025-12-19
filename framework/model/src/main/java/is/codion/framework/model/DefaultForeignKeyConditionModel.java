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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
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

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private final ForeignKey foreignKey;
	private final ConditionModel<Entity> condition;
	private final @Nullable EntitySearchModel equalSearchModel;
	private final @Nullable EntitySearchModel inSearchModel;

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		foreignKey = builder.foreignKey;
		equalSearchModel = builder.equalSearchModel;
		inSearchModel = builder.inSearchModel;
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						.operators(builder.operators())
						.operator(builder.inSearchModel == null ? Operator.EQUAL : Operator.IN)
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
	public EntitySearchModel equalSearchModel() {
		if (equalSearchModel == null) {
			throw new IllegalStateException("No EntitySearchModel available for the EQUAL operand");
		}

		return equalSearchModel;
	}

	@Override
	public EntitySearchModel inSearchModel() {
		if (inSearchModel == null) {
			throw new IllegalStateException("No EntitySearchModel available for the IN operand");
		}

		return inSearchModel;
	}

	static final class DefaultBuilder implements Builder {

		private final ForeignKey foreignKey;

		private @Nullable EntitySearchModel equalSearchModel;
		private @Nullable EntitySearchModel inSearchModel;

		DefaultBuilder(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public Builder equalSearchModel(EntitySearchModel equalSearchModel) {
			this.equalSearchModel = requireNonNull(equalSearchModel);
			return this;
		}

		@Override
		public Builder inSearchModel(EntitySearchModel inSearchModel) {
			this.inSearchModel = requireNonNull(inSearchModel);
			return this;
		}

		@Override
		public ForeignKeyConditionModel build() {
			return new DefaultForeignKeyConditionModel(this);
		}

		private List<Operator> operators() {
			if (equalSearchModel == null && inSearchModel == null) {
				throw new IllegalStateException("Neither EQUAL nor IN operator specified");
			}
			if (equalSearchModel != null && inSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN);
			}
			if (equalSearchModel != null) {
				return asList(Operator.EQUAL, Operator.NOT_EQUAL);
			}

			return asList(Operator.IN, Operator.NOT_IN);
		}
	}

	private final class ForeignKeyOperands implements Operands<Entity> {

		@Override
		public Value<Entity> equal() {
			return equalSearchModel == null ? Operands.super.equal() : equalSearchModel.selection().entity();
		}

		@Override
		public ValueSet<Entity> in() {
			return inSearchModel == null ? Operands.super.in() : inSearchModel.selection().entities();
		}
	}
}
