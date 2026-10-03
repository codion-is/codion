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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultColumnConditionModel<T> implements ColumnConditionModel<T> {

	private final Column<T> column;
	private final ConditionModel<T> condition;

	private DefaultColumnConditionModel(DefaultBuilder<T> builder) {
		column = builder.columnDefinition.attribute();
		ConditionModel.Builder<T> conditionBuilder = ConditionModel.builder()
						.type(column.type().get())
						.format(builder.columnDefinition.format().orElse(null))
						.caption(builder.columnDefinition.caption())
						.dateTimePattern(builder.columnDefinition.dateTimePattern().orElse(null))
						.operands(new AttributeOperands<>(builder.columnDefinition));
		if (builder.operators != null) {
			conditionBuilder.operators(builder.operators)
							.operator(builder.operator == null ? builder.operators.get(0) : builder.operator);
		}
		else if (builder.operator != null) {
			conditionBuilder.operator(builder.operator);
		}
		condition = conditionBuilder.build();
	}

	@Override
	public Column<T> attribute() {
		return column;
	}

	@Override
	public ConditionModel<T> condition() {
		return condition;
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public Optional<String> caption() {
		return condition.caption();
	}

	static <T> DefaultBuilder<T> builder(ColumnDefinition<T> columnDefinition) {
		return new DefaultBuilder<>(requireNonNull(columnDefinition));
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		private final ColumnDefinition<T> columnDefinition;

		private @Nullable List<Operator> operators;
		private @Nullable Operator operator;
		private boolean negationIncludesNull = EntityConditions.NEGATION_INCLUDES_NULL.getOrThrow();

		private DefaultBuilder(ColumnDefinition<T> columnDefinition) {
			this.columnDefinition = columnDefinition;
		}

		@Override
		public Builder<T> operators(List<Operator> operators) {
			if (requireNonNull(operators).isEmpty()) {
				throw new IllegalArgumentException("No operators specified");
			}
			this.operators = unmodifiableList(new ArrayList<>(operators));
			return this;
		}

		@Override
		public Builder<T> operator(Operator operator) {
			this.operator = requireNonNull(operator);
			return this;
		}

		@Override
		public Builder<T> negationIncludesNull(boolean negationIncludesNull) {
			this.negationIncludesNull = negationIncludesNull;
			return this;
		}

		boolean negationIncludesNull() {
			return negationIncludesNull;
		}

		@Override
		public ColumnConditionModel<T> build() {
			return new DefaultColumnConditionModel<>(this);
		}
	}
}
