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
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import static is.codion.common.reactive.value.Value.Notify.SET;

final class DefaultColumnConditionModel<T> implements ColumnConditionModel<T> {

	private final Column<T> column;
	private final ConditionModel<T> condition;

	private DefaultColumnConditionModel(DefaultBuilder<T> builder) {
		column = builder.columnDefinition.attribute();
		condition = ConditionModel.builder()
						.valueClass(column.type().valueClass())
						.format(builder.columnDefinition.format().orElse(null))
						.dateTimePattern(builder.columnDefinition.dateTimePattern().orElse(null))
						.operands(new ColumnOperands<>(builder.columnDefinition))
						.build();
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

	static final class DefaultBuilder<T> implements Builder<T> {

		private final ColumnDefinition<T> columnDefinition;

		DefaultBuilder(ColumnDefinition<T> columnDefinition) {
			this.columnDefinition = columnDefinition;
		}

		@Override
		public ColumnConditionModel<T> build() {
			return new DefaultColumnConditionModel<>(this);
		}
	}

	private static final class ColumnOperands<T> implements Operands<T> {

		private final ColumnDefinition<T> definition;

		private ColumnOperands(ColumnDefinition<T> definition) {
			this.definition = definition;
		}

		@Override
		public Value<T> equal() {
			if (definition.attribute().type().isBoolean() && !definition.nullable()) {
				return (Value<T>) Value.builder()
								.nonNull(false)
								.notify(SET)
								.build();
			}

			return Operands.super.equal();
		}
	}
}
