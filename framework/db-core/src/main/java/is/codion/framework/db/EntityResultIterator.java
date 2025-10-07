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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.domain.entity.Entity;

import java.util.Iterator;

/**
 * Iterates through an {@link Entity} based query result.
 * Use try with resources or remember to call {@link #close()} in order to close underlying resources.
 * {@snippet :
 * try (EntityResultIterator iterator = connection.iterator(select)) {
 *   while (iterator.hasNext()) {
 *     Entity entity = iterator.next();
 *     // process entity
 *   }
 * }
 *}
 */
public interface EntityResultIterator extends Iterator<Entity>, Iterable<Entity>, AutoCloseable {

	@Override
	void close();

	@Override
	default Iterator<Entity> iterator() {
		return this;
	}
}
