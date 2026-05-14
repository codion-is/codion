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

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.AbstractObserver;
import is.codion.common.reactive.observer.Observer;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

final class DefaultPersistenceEvents implements PersistenceEvents {

	static final Map<EntityType, PersistenceEvents> EVENTS = new ConcurrentHashMap<>();

	private final EntityType entityType;

	private final Inserted inserted = new DefaultInsertEvent();
	private final Updated updated = new DefaultUpdated();
	private final Deleted deleted = new DefaultDeleted();
	private final Event<Collection<Entity>> persisted = Event.event();

	DefaultPersistenceEvents(EntityType entityType) {
		this.entityType = entityType;
	}

	@Override
	public Inserted inserted() {
		return inserted;
	}

	@Override
	public Updated updated() {
		return updated;
	}

	@Override
	public Deleted deleted() {
		return deleted;
	}

	@Override
	public Observer<Collection<Entity>> persisted() {
		return persisted.observer();
	}

	private void validate(Collection<Entity> entities) {
		for (Entity entity : requireNonNull(entities)) {
			if (!entity.type().equals(entityType)) {
				throw new IllegalArgumentException("Invalid entity type for edit notification: " + entity.type() + ", expected " + entityType);
			}
		}
	}

	private final class DefaultInsertEvent extends AbstractObserver<Collection<Entity>> implements Inserted {

		@Override
		public void accept(Collection<Entity> inserted) {
			validate(inserted);
			notifyListeners(inserted);
			persisted.accept(inserted);
		}
	}

	private final class DefaultUpdated extends AbstractObserver<Map<Entity, Entity>> implements Updated {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			validate(requireNonNull(updated).keySet());
			validate(updated.values());
			notifyListeners(updated);
			persisted.accept(updated.values());
		}
	}

	private final class DefaultDeleted extends AbstractObserver<Collection<Entity>> implements Deleted {

		@Override
		public void accept(Collection<Entity> deleted) {
			validate(deleted);
			notifyListeners(deleted);
			persisted.accept(deleted);
		}
	}
}
