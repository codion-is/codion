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

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.stream.StreamSupport.stream;

/**
 * Provides String representations of {@link Select} instances, for debugging.
 */
public interface EntityQueries {

	/**
	 * @param select the {@link Select} instance
	 * @return a String representation of the given {@link Select} instance, for debugging.
	 */
	String select(Select select);

	/**
	 * @param entity the entity
	 * @return a String representation of the insert query for the given entity, for debugging
	 */
	String insert(Entity entity);

	/**
	 * Note that the query returned by this method is only guaranteed
	 * to be valid if the given entity is modified.
	 * @param entity the entity
	 * @return a String representation of the update query for the given entity, for debugging
	 * @see Entity#modified()
	 */
	String update(Entity entity);

	/**
	 * Returns the first {@link Factory} implementation found by the {@link ServiceLoader}.
	 * @return a {@link Factory} implementation from the {@link ServiceLoader} or an empty {@link Optional} in case none is available.
	 */
	static Optional<Factory> instance() {
		try {
			return stream(ServiceLoader.load(Factory.class).spliterator(), false).findFirst();
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}

	/**
	 * A factory for {@link EntityQueries} instances
	 */
	interface Factory {

		/**
		 * @param database the database
		 * @param entities the entities
		 * @return a new {@link EntityQueries} instance based on the given database and entities
		 */
		EntityQueries create(Database database, Entities entities);
	}
}