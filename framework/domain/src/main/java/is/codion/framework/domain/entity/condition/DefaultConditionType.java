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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultConditionType implements ConditionType, Serializable {

	private static final long serialVersionUID = 1;

	private final EntityType entityType;
	private final String name;

	DefaultConditionType(EntityType entityType, String name) {
		this.entityType = requireNonNull(entityType);
		this.name = requireNonNull(name);
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		DefaultConditionType that = (DefaultConditionType) object;
		return entityType.equals(that.entityType) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(entityType, name);
	}
}
