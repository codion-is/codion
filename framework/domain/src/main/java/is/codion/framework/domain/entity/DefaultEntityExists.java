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
package is.codion.framework.domain.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Predicate;

final class DefaultEntityExists implements Predicate<Entity>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	@Override
	public boolean test(Entity entity) {
		return !entity.originalPrimaryKey().isNull();
	}
}
