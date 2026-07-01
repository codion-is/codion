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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;

import static is.codion.common.reactive.value.Value.Notify.SET;

/**
 * {@link Operands} derived from a {@link ValueAttributeDefinition}: a non-nullable boolean attribute gets a non-null
 * {@code false} 'equal' operand, so a fresh condition on it matches {@code false} rather than an unsatisfiable
 * {@code null} (the column is never null). Shared by the search condition model ({@link DefaultColumnConditionModel})
 * and the table filter condition models ({@link AbstractEntityTableModel}), so search and filter initialize identically.
 */
final class AttributeOperands<T> implements Operands<T> {

	private final ValueAttributeDefinition<T> definition;

	AttributeOperands(ValueAttributeDefinition<T> definition) {
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
