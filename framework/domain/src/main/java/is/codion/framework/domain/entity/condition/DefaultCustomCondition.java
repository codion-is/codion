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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition extends AbstractCondition implements CustomCondition {

	private static final long serialVersionUID = 1;

	private final ConditionType conditionType;

	DefaultCustomCondition(ConditionType conditionType, List<Column<?>> columns, List<Object> values) {
		super(requireNonNull(conditionType).entityType(), columns, values);
		this.conditionType = conditionType;
	}

	@Override
	public ConditionType conditionType() {
		return conditionType;
	}

	@Override
	public String toString(EntityDefinition definition) {
		return requireNonNull(definition).conditionProvider(conditionType).toString(columns(), values());
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultCustomCondition)) {
			return false;
		}
		if (!super.equals(object)) {
			return false;
		}
		DefaultCustomCondition that = (DefaultCustomCondition) object;
		return conditionType.equals(that.conditionType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), conditionType);
	}

	@Override
	public String toString() {
		return "DefaultCustomCondition{" +
						"conditionType=" + conditionType +
						", columns=" + columns() +
						", values=" + values() + "}";
	}
}
