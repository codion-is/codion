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

import is.codion.common.Operator;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;

import java.text.Format;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private final ConditionModel<Entity> condition;
	private final ForeignKeyOperands operands;

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		this.condition = ConditionModel.builder(Entity.class)
						.operators(builder.operators())
						.operator(builder.inSearchModel == null ? Operator.EQUAL : Operator.IN)
						.build();
		this.operands = new DefaultForeignKeyOperands(condition, builder.equalSearchModel, builder.inSearchModel);
	}

	@Override
	public State caseSensitive() {
		return condition.caseSensitive();
	}

	@Override
	public Optional<Format> format() {
		return condition.format();
	}

	@Override
	public Optional<String> dateTimePattern() {
		return condition.dateTimePattern();
	}

	@Override
	public Value<Wildcard> wildcard() {
		return condition.wildcard();
	}

	@Override
	public State autoEnable() {
		return condition.autoEnable();
	}

	@Override
	public State locked() {
		return condition.locked();
	}

	@Override
	public Class<Entity> valueClass() {
		return condition.valueClass();
	}

	@Override
	public List<Operator> operators() {
		return condition.operators();
	}

	@Override
	public State enabled() {
		return condition.enabled();
	}

	@Override
	public void clear() {
		condition.clear();
	}

	@Override
	public Value<Operator> operator() {
		return condition.operator();
	}

	@Override
	public ForeignKeyOperands operands() {
		return operands;
	}

	@Override
	public boolean accepts(Comparable<Entity> value) {
		return condition.accepts(value);
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	private static final class DefaultForeignKeyOperands implements ForeignKeyOperands {

		private final ConditionModel<Entity> condition;
		private final EntitySearchModel equalSearchModel;
		private final EntitySearchModel inSearchModel;

		private DefaultForeignKeyOperands(ConditionModel<Entity> condition,
																			EntitySearchModel equalSearchModel,
																			EntitySearchModel inSearchModel) {
			this.condition = requireNonNull(condition);
			this.equalSearchModel = equalSearchModel;
			this.inSearchModel = inSearchModel;
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

		@Override
		public Value<Entity> equal() {
			return condition.operands().equal();
		}

		@Override
		public ValueSet<Entity> in() {
			return condition.operands().in();
		}

		@Override
		public Value<Entity> upper() {
			return condition.operands().upper();
		}

		@Override
		public Value<Entity> lower() {
			return condition.operands().lower();
		}
	}

	static final class DefaultBuilder implements Builder {

		private EntitySearchModel equalSearchModel;
		private EntitySearchModel inSearchModel;

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
}
