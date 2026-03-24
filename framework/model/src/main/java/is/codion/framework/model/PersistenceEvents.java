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

import is.codion.common.reactive.observer.Observer;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * @see EntityEditor#PUBLISH_PERSISTENCE_EVENTS
 * @see EntityEditor.Settings#publishPersistenceEvents()
 * @see #persistenceEvents(EntityType)
 */
public interface PersistenceEvents {

	/**
	 * @param entityType the entity type
	 * @return the central {@link PersistenceEvents} instance for the given entity type
	 */
	static PersistenceEvents persistenceEvents(EntityType entityType) {
		return DefaultPersistenceEvents.EVENTS.computeIfAbsent(requireNonNull(entityType), DefaultPersistenceEvents::new);
	}

	/**
	 * Returns an {@link Inserted}, notified each time entities are inserted.
	 * @return the {@link Inserted} instance
	 */
	Inserted inserted();

	/**
	 * Returns an {@link Updated}, notified each time entities are updated.
	 * @return the {@link Updated} instance
	 */
	Updated updated();

	/**
	 * Returns a {@link Deleted}, notified each time entities are deleted.
	 * @return the {@link Deleted} instance
	 */
	Deleted deleted();

	/**
	 * Notified on insert.
	 */
	interface Inserted extends Observer<Collection<Entity>>, Consumer<Collection<Entity>> {

		/**
		 * Notifies that the given entities have been inserted.
		 * @param inserted the inserted entities
		 * @throws IllegalArgumentException in case any of the entities is of the incorrect type
		 */
		void accept(Collection<Entity> inserted);
	}

	/**
	 * Notified on update.
	 */
	interface Updated extends Observer<Map<Entity, Entity>>, Consumer<Map<Entity, Entity>> {

		/**
		 * Notifies that the given entities have been updated.
		 * @param updated the updated entities, mapped to their original state
		 * @throws IllegalArgumentException in case any of the entities is of the incorrect type
		 */
		void accept(Map<Entity, Entity> updated);
	}

	/**
	 * Notified on delete.
	 */
	interface Deleted extends Observer<Collection<Entity>>, Consumer<Collection<Entity>> {

		/**
		 * Notifies that the given entities have been deleted.
		 * @param deleted the deleted entities
		 * @throws IllegalArgumentException in case any of the entities is of the incorrect type
		 */
		void accept(Collection<Entity> deleted);
	}
}
