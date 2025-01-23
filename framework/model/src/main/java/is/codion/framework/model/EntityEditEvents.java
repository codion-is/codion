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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.event.Event.event;
import static is.codion.framework.domain.entity.Entity.groupByType;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A central event hub for listening for entity inserts, updates and deletes.
 * {@link EntityEditModel} uses this to post its events.
 * @see EntityEditModel#POST_EDIT_EVENTS
 * @see EntityEditModel#postEditEvents()
 */
public final class EntityEditEvents {

	private static final EntityEditListener EDIT_LISTENER = new EntityEditListener();

	private EntityEditEvents() {}

	/**
	 * Returns an insert observer, notified each time entities of the given type are inserted.
	 * @param entityType the type of entity to listen for
	 * @return the insert observer for the given entity type
	 */
	public static Observer<Collection<Entity>> insertObserver(EntityType entityType) {
		return EDIT_LISTENER.insertObserver(requireNonNull(entityType));
	}

	/**
	 * Returns an update observer, notified each time entities of the given type are updated.
	 * @param entityType the type of entity to listen for
	 * @return the update observer for the given entity type
	 */
	public static Observer<Map<Entity.Key, Entity>> updateObserver(EntityType entityType) {
		return EDIT_LISTENER.updateObserver(requireNonNull(entityType));
	}

	/**
	 * Returns a delete observer, notified each time entities of the given type are deleted.
	 * @param entityType the type of entity to listen for
	 * @return the delete observer for the given entity type
	 */
	public static Observer<Collection<Entity>> deleteObserver(EntityType entityType) {
		return EDIT_LISTENER.deleteObserver(requireNonNull(entityType));
	}

	/**
	 * Notifies insert
	 * @param insertedEntities the inserted entities
	 */
	public static void inserted(Collection<Entity> insertedEntities) {
		EDIT_LISTENER.notifyInserted(requireNonNull(insertedEntities));
	}

	/**
	 * Notifies update
	 * @param updatedEntities the updated entities mapped to their original primary key
	 */
	public static void updated(Map<Entity.Key, Entity> updatedEntities) {
		EDIT_LISTENER.notifyUpdated(requireNonNull(updatedEntities));
	}

	/**
	 * Notifies delete
	 * @param deletedEntities the deleted entities
	 */
	public static void deleted(Collection<Entity> deletedEntities) {
		EDIT_LISTENER.notifyDeleted(requireNonNull(deletedEntities));
	}

	private static final class EntityEditListener {

		private final Map<EntityType, Event<Collection<Entity>>> insertEvents = synchronizedMap(new LinkedHashMap<>());
		private final Map<EntityType, Event<Map<Entity.Key, Entity>>> updateEvents = synchronizedMap(new LinkedHashMap<>());
		private final Map<EntityType, Event<Collection<Entity>>> deleteEvents = synchronizedMap(new LinkedHashMap<>());

		private Observer<Collection<Entity>> insertObserver(EntityType entityType) {
			return insertEvents.computeIfAbsent(entityType, k -> event()).observer();
		}

		private Observer<Map<Entity.Key, Entity>> updateObserver(EntityType entityType) {
			return updateEvents.computeIfAbsent(entityType, k -> event()).observer();
		}

		private Observer<Collection<Entity>> deleteObserver(EntityType entityType) {
			return deleteEvents.computeIfAbsent(entityType, k -> event()).observer();
		}

		private void notifyInserted(Collection<Entity> inserted) {
			groupByType(inserted).forEach(this::notifyInserted);
		}

		private void notifyInserted(EntityType entityType, Collection<Entity> inserted) {
			Event<Collection<Entity>> event = insertEvents.get(entityType);
			if (event != null) {
				event.accept(inserted);
			}
		}

		private void notifyUpdated(Map<Entity.Key, Entity> updated) {
			updated.entrySet()
							.stream()
							.collect(groupingBy(entry -> entry.getKey().type(), LinkedHashMap::new, toList()))
							.forEach(this::notifyUpdated);
		}

		private void notifyUpdated(EntityType entityType, List<Map.Entry<Entity.Key, Entity>> updated) {
			Event<Map<Entity.Key, Entity>> event = updateEvents.get(entityType);
			if (event != null) {
				event.accept(updated.stream()
								.collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
			}
		}

		private void notifyDeleted(Collection<Entity> deleted) {
			groupByType(deleted).forEach(this::notifyDeleted);
		}

		private void notifyDeleted(EntityType entityType, Collection<Entity> deleted) {
			Event<Collection<Entity>> event = deleteEvents.get(entityType);
			if (event != null) {
				event.accept(deleted);
			}
		}
	}
}
