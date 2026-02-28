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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditor.PersistTask;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public class DefaultEntityEditModel implements EntityEditModel {

	private final EntityEditor editor;
	private final DefaultPersistTasks tasks;
	private final DefaultPersistEvents events;
	private final DefaultSettings settings;

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	public DefaultEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new DefaultEntityEditor(entityType, connectionProvider));
	}

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param editor the editor
	 */
	public DefaultEntityEditModel(EntityEditor editor) {
		this.editor = requireNonNull(editor);
		this.settings = new DefaultSettings(editor.entityDefinition().readOnly());
		this.tasks = new DefaultPersistTasks();
		this.events = new DefaultPersistEvents(settings.publishPersistenceEvents);
	}

	@Override
	public final Entities entities() {
		return editor.entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return editor.entityDefinition();
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entityType();
	}

	@Override
	public final Settings settings() {
		return settings;
	}

	@Override
	public final EntityType entityType() {
		return editor.entityDefinition().type();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editor.connectionProvider();
	}

	@Override
	public final EntityConnection connection() {
		return editor.connectionProvider().connection();
	}

	@Override
	public EntityEditor editor() {
		return editor;
	}

	@Override
	public final PersistTasks tasks() {
		return tasks;
	}

	@Override
	public final PersistEvents events() {
		return events;
	}

	@Override
	public final Entity insert() throws EntityValidationException {
		return tasks.insert().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) throws EntityValidationException {
		return tasks.insert(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() throws EntityValidationException {
		return tasks.update().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) throws EntityValidationException {
		return tasks.update(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() {
		return tasks.delete().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) {
		return tasks.delete(entities).prepare().perform().handle();
	}

	private final class DefaultPersistTasks implements PersistTasks {

		@Override
		public PersistTask insert() throws EntityValidationException {
			settings.verifyInsertEnabled();

			return editor.tasks().insert()
							.before(events.beforeInsert)
							.after(events::inserted)
							.build();
		}

		@Override
		public PersistTask insert(Entity entity) throws EntityValidationException {
			return insert(singleton(requireNonNull(entity)));
		}

		@Override
		public PersistTask insert(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyInsertEnabled();

			return editor.tasks().insert(entities)
							.before(events.beforeInsert)
							.after(events::inserted)
							.build();
		}

		@Override
		public PersistTask update() throws EntityValidationException {
			settings.verifyUpdateEnabled(1);

			return editor.tasks().update()
							.before(events.beforeUpdate)
							.after(events::updated)
							.build();
		}

		@Override
		public PersistTask update(Entity entity) throws EntityValidationException {
			return update(singleton(requireNonNull(entity)));
		}

		@Override
		public PersistTask update(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyUpdateEnabled(requireNonNull(entities).size());

			return editor.tasks().update(entities)
							.before(events.beforeUpdate)
							.after(events::updated)
							.build();
		}

		@Override
		public PersistTask delete() {
			settings.verifyDeleteEnabled();

			return editor.tasks().delete()
							.before(events.beforeDelete)
							.after(events::deleted)
							.build();
		}

		@Override
		public PersistTask delete(Entity entity) {
			return delete(singleton(requireNonNull(entity)));
		}

		@Override
		public PersistTask delete(Collection<Entity> entities) {
			settings.verifyDeleteEnabled();

			return editor.tasks().delete(entities)
							.before(events.beforeDelete)
							.after(events::deleted)
							.build();
		}
	}

	private static final class DefaultPersistEvents implements PersistEvents {

		private final Event<Collection<Entity>> beforeInsert = Event.event();
		private final Event<Collection<Entity>> afterInsert = Event.event();
		private final Event<Collection<Entity>> beforeUpdate = Event.event();
		private final Event<Map<Entity, Entity>> afterUpdate = Event.event();
		private final Event<Collection<Entity>> beforeDelete = Event.event();
		private final Event<Collection<Entity>> afterDelete = Event.event();
		private final Event<Collection<Entity>> persisted = Event.event();

		private final ObservableState publishPersistenceEvents;

		private DefaultPersistEvents(ObservableState publishPersistenceEvents) {
			this.publishPersistenceEvents = publishPersistenceEvents;
		}

		@Override
		public Observer<Collection<Entity>> beforeInsert() {
			return beforeInsert.observer();
		}

		@Override
		public Observer<Collection<Entity>> afterInsert() {
			return afterInsert.observer();
		}

		@Override
		public Observer<Collection<Entity>> beforeUpdate() {
			return beforeUpdate.observer();
		}

		@Override
		public Observer<Map<Entity, Entity>> afterUpdate() {
			return afterUpdate.observer();
		}

		@Override
		public Observer<Collection<Entity>> beforeDelete() {
			return beforeDelete.observer();
		}

		@Override
		public Observer<Collection<Entity>> afterDelete() {
			return afterDelete.observer();
		}

		@Override
		public Observer<Collection<Entity>> persisted() {
			return persisted.observer();
		}

		private void inserted(Collection<Entity> inserted) {
			requireNonNull(inserted);
			afterInsert.accept(inserted);
			persisted.accept(inserted);
			if (publishPersistenceEvents.is()) {
				DefaultEntityEditModel.notifyInserted(inserted);
			}
		}

		private void updated(Map<Entity, Entity> updated) {
			requireNonNull(updated);
			afterUpdate.accept(updated);
			persisted.accept(updated.values());
			if (publishPersistenceEvents.is()) {
				DefaultEntityEditModel.notifyUpdated(updated);
			}
		}

		private void deleted(Collection<Entity> deleted) {
			requireNonNull(deleted);
			afterDelete.accept(deleted);
			persisted.accept(deleted);
			if (publishPersistenceEvents.is()) {
				DefaultEntityEditModel.notifyDeleted(deleted);
			}
		}
	}

	private static final class DefaultSettings implements Settings {

		private final State readOnly;
		private final State insertEnabled = State.state(true);
		private final State updateEnabled = State.state(true);
		private final State updateMultipleEnabled = State.state(true);
		private final State deleteEnabled = State.state(true);
		private final State publishPersistenceEvents = State.state(PUBLISH_PERSISTENCE_EVENTS.getOrThrow());

		private DefaultSettings(boolean readOnly) {
			this.readOnly = State.state(readOnly);
		}

		@Override
		public State publishPersistenceEvents() {
			return publishPersistenceEvents;
		}

		@Override
		public State readOnly() {
			return readOnly;
		}

		@Override
		public State insertEnabled() {
			return insertEnabled;
		}

		@Override
		public State updateEnabled() {
			return updateEnabled;
		}

		@Override
		public State updateMultipleEnabled() {
			return updateMultipleEnabled;
		}

		@Override
		public State deleteEnabled() {
			return deleteEnabled;
		}

		private void verifyInsertEnabled() {
			verifyNotReadOnly();
			if (!insertEnabled.is()) {
				throw new IllegalStateException("Inserting is not enabled!");
			}
		}

		private void verifyUpdateEnabled(int entityCount) {
			verifyNotReadOnly();
			if (!updateEnabled.is()) {
				throw new IllegalStateException("Updating is not enabled!");
			}
			if (entityCount > 1 && !updateMultipleEnabled.is()) {
				throw new IllegalStateException("Updating multiple entities is not enabled");
			}
		}

		private void verifyDeleteEnabled() {
			verifyNotReadOnly();
			if (!deleteEnabled.is()) {
				throw new IllegalStateException("Deleting is not enabled!");
			}
		}

		private void verifyNotReadOnly() {
			if (readOnly.is()) {
				throw new IllegalStateException("Edit model is read-only!");
			}
		}
	}

	private static void notifyInserted(Collection<Entity> inserted) {
		groupByType(inserted).forEach((entityType, entities) ->
						persistenceEvents(entityType).inserted().accept(entities));
	}

	private static void notifyUpdated(Map<Entity, Entity> updated) {
		updated.entrySet().stream()
						.collect(groupingBy(entry -> entry.getKey().type(), LinkedHashMap::new,
										toMap(Map.Entry::getKey, Map.Entry::getValue)))
						.forEach((entityType, entities) ->
										persistenceEvents(entityType).updated().accept(entities));
	}

	private static void notifyDeleted(Collection<Entity> deleted) {
		groupByType(deleted).forEach((entityType, entities) ->
						persistenceEvents(entityType).deleted().accept(entities));
	}
}