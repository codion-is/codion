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

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

/**
 * <p>Responsible for persisting entities to the database.
 * <p>Set on an editor via {@link EntityEditor#persistence()}, or provided for an entity type by a {@link Factory}
 * registered for its domain, reaching every editor of that type, those the framework creates itself included, such
 * as the ones of a dependencies view.
 */
public interface EntityPersistence {

	/**
	 * <p>Inserts the given entity into the database using the given connection.
	 * @param entity the entity to insert
	 * @param connection the connection to use
	 * @return the inserted entity
	 * @throws DatabaseException in case of a database exception
	 */
	default Entity insert(Entity entity, EntityConnection connection) {
		return insert(singleton(requireNonNull(entity)), connection).iterator().next();
	}

	/**
	 * Inserts the given entities into the database using the given connection
	 * @param entities the entities to insert
	 * @param connection the connection to use
	 * @return the inserted entities
	 * @throws DatabaseException in case of a database exception
	 */
	default Collection<Entity> insert(Collection<Entity> entities, EntityConnection connection) {
		return requireNonNull(connection).insertSelect(entities);
	}

	/**
	 * <p>Updates the given entity in the database using the given connection.
	 * @param entity the entity to update
	 * @param connection the connection to use
	 * @return the updated entity
	 * @throws DatabaseException in case of a database exception
	 */
	default Entity update(Entity entity, EntityConnection connection) {
		return update(singleton(requireNonNull(entity)), connection).iterator().next();
	}

	/**
	 * Updates the given entities in the database using the given connection
	 * @param entities the entities to update
	 * @param connection the connection to use
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 */
	default Collection<Entity> update(Collection<Entity> entities, EntityConnection connection) {
		return requireNonNull(connection).updateSelect(entities);
	}

	/**
	 * <p>Deletes the given entity from the database using the given connection.
	 * @param entity the entity to delete
	 * @param connection the connection to use
	 * @throws DatabaseException in case of a database exception
	 */
	default void delete(Entity entity, EntityConnection connection) {
		delete(singleton(requireNonNull(entity)), connection);
	}

	/**
	 * Deletes the given entities from the database using the given connection
	 * @param entities the entities to delete
	 * @param connection the connection to use
	 * @throws DatabaseException in case of a database exception
	 */
	default void delete(Collection<Entity> entities, EntityConnection connection) {
		requireNonNull(connection).delete(Entity.primaryKeys(entities));
	}

	/**
	 * <p>Provides the {@link EntityPersistence} of entity types in a domain, the default of every editor of those types,
	 * whoever creates it. One set on an editor via {@link EntityEditor#persistence()} overrides it for that editor.
	 * <p>Found via {@link java.util.ServiceLoader}, the first one supporting the domain, resolved once per domain.
	 */
	interface Factory {

		/**
		 * Called for each editor created for an entity type of a compatible domain.
		 * @param entityType the entity type
		 * @return the persistence of the given entity type, an empty Optional for the default
		 */
		Optional<EntityPersistence> persistence(EntityType entityType);

		/**
		 * @param domainType the domain type
		 * @return true if this factory provides the persistence of entity types in the given domain
		 */
		boolean supports(DomainType domainType);
	}
}
