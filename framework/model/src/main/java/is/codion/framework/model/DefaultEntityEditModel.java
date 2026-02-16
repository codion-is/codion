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
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public class DefaultEntityEditModel implements EntityEditModel {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

	private final EntityEditor editor;
	private final EntityPersistence persistence;
	private final Events events;
	private final DefaultSettings settings;

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	public DefaultEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new DefaultEntityEditor(entityType, connectionProvider), new EntityPersistence() {});
	}

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 * @param persistence the persistence implementation
	 */
	public DefaultEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																EntityPersistence persistence) {
		this(new DefaultEntityEditor(entityType, connectionProvider), persistence);
	}

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param editor the editor
	 */
	public DefaultEntityEditModel(EntityEditor editor) {
		this(editor, new EntityPersistence() {});
	}

	/**
	 * Instantiates a new {@link DefaultEntityEditModel} based on the given editor
	 * @param editor the editor
	 * @param persistence the persistence implementation
	 */
	public DefaultEntityEditModel(EntityEditor editor, EntityPersistence persistence) {
		this.editor = requireNonNull(editor);
		this.persistence = requireNonNull(persistence);
		this.settings = new DefaultSettings(editor.entityDefinition().readOnly());
		this.events = new Events(settings.publishPersistenceEvents);
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
	public final EntityPersistence persistence() {
		return persistence;
	}

	@Override
	public final Entity insert() {
		return insertTask().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) {
		return insertTask(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() {
		return updateTask().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) {
		return updateTask(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() {
		return deleteTask().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) {
		return deleteTask(entities).prepare().perform().handle();
	}

	@Override
	public final EditTask insertTask() {
		return new InsertEntity();
	}

	@Override
	public final EditTask insertTask(Collection<Entity> entities) {
		return new InsertEntities(entities);
	}

	@Override
	public final EditTask updateTask() {
		return new UpdateEntity();
	}

	@Override
	public final EditTask updateTask(Collection<Entity> entities) {
		return new UpdateEntities(entities);
	}

	@Override
	public final EditTask deleteTask() {
		return new DeleteEntity();
	}

	@Override
	public final EditTask deleteTask(Collection<Entity> entities) {
		return new DeleteEntities(entities);
	}

	@Override
	public final Observer<Collection<Entity>> beforeInsert() {
		return events.beforeInsert.observer();
	}

	@Override
	public final Observer<Collection<Entity>> afterInsert() {
		return events.afterInsert.observer();
	}

	@Override
	public final Observer<Collection<Entity>> beforeUpdate() {
		return events.beforeUpdate.observer();
	}

	@Override
	public final Observer<Map<Entity, Entity>> afterUpdate() {
		return events.afterUpdate.observer();
	}

	@Override
	public final Observer<Collection<Entity>> beforeDelete() {
		return events.beforeDelete.observer();
	}

	@Override
	public final Observer<Collection<Entity>> afterDelete() {
		return events.afterDelete.observer();
	}

	@Override
	public final Observer<Collection<Entity>> persisted() {
		return events.persisted.observer();
	}

	/**
	 * Notifies that insert is about to be performed
	 * @param entitiesToInsert the entities about to be inserted
	 * @see #beforeInsert()
	 */
	protected final void notifyBeforeInsert(Collection<Entity> entitiesToInsert) {
		events.beforeInsert.accept(requireNonNull(entitiesToInsert));
	}

	/**
	 * Notifies that insert has been performed
	 * @param insertedEntities the inserted entities
	 * @see #afterInsert()
	 */
	protected final void notifyAfterInsert(Collection<Entity> insertedEntities) {
		events.notifyInserted(requireNonNull(insertedEntities));
	}

	/**
	 * Notifies that update is about to be performed
	 * @param entitiesToUpdate the entities about to be updated
	 * @see #beforeUpdate()
	 */
	protected final void notifyBeforeUpdate(Collection<Entity> entitiesToUpdate) {
		events.beforeUpdate.accept(requireNonNull(entitiesToUpdate));
	}

	/**
	 * Notifies that update has been performed
	 * @param updatedEntities a map containing the updated entities, mapped to their state before the update
	 * @see #afterUpdate()
	 */
	protected final void notifyAfterUpdate(Map<Entity, Entity> updatedEntities) {
		events.notifyUpdated(requireNonNull(updatedEntities));
	}

	/**
	 * Notifies that delete is about to be performed
	 * @param entitiesToDelete the entities about to be deleted
	 * @see #beforeDelete()
	 */
	protected final void notifyBeforeDelete(Collection<Entity> entitiesToDelete) {
		events.beforeDelete.accept(requireNonNull(entitiesToDelete));
	}

	/**
	 * Notifies that delete has been performed
	 * @param deletedEntities the deleted entities
	 * @see #afterDelete()
	 */
	protected final void notifyAfterDelete(Collection<Entity> deletedEntities) {
		events.notifyDeleted(requireNonNull(deletedEntities));
	}

	/**
	 * Maps the given entities and their updated counterparts, assumes a single copy of each entity in the given lists.
	 * @param entitiesBeforeUpdate the entities before update
	 * @param entitiesAfterUpdate the entities after update
	 * @return the updated entities mapped to their respective state before the update
	 */
	private static Map<Entity, Entity> originalEntityMap(Collection<Entity> entitiesBeforeUpdate,
																											 Collection<Entity> entitiesAfterUpdate) {
		List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
		Map<Entity, Entity> entityMap = new HashMap<>(entitiesBeforeUpdate.size());
		for (Entity entity : entitiesBeforeUpdate) {
			entityMap.put(entity.immutable(), findAndRemove(entity.primaryKey(), entitiesAfterUpdateCopy.listIterator()));
		}

		return unmodifiableMap(entityMap);
	}

	private static Entity findAndRemove(Entity.Key primaryKey, ListIterator<Entity> iterator) {
		while (iterator.hasNext()) {
			Entity current = iterator.next();
			if (current.primaryKey().equals(primaryKey)) {
				iterator.remove();

				return current;
			}
		}

		throw new IllegalStateException("Updated entity not found");
	}

	private final class InsertEntity implements EditTask {

		private final Entity entity = activeEntity();

		private InsertEntity() {
			settings.verifyInsertEnabled();
			editor.validate(entity);
		}

		@Override
		public Task prepare() {
			notifyBeforeInsert(singleton(entity));

			return new InsertTask();
		}

		private Entity activeEntity() {
			Entity.Builder builder = editor.entity().get().copy().builder();
			editor.entityDefinition().columns().definitions().stream()
							.filter(ColumnDefinition::primaryKey)
							.filter(ColumnDefinition::generated)
							.forEach(column -> builder.clear(column.attribute()));

			return builder.build();
		}

		private final class InsertTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - insert {}", DefaultEntityEditModel.this, entity);

				return new InsertResult(persistence.insert(entity, connection()));
			}
		}

		private final class InsertResult implements Result {

			private final Entity insertedEntity;

			private InsertResult(Entity insertedEntity) {
				this.insertedEntity = insertedEntity;
			}

			@Override
			public Collection<Entity> handle() {
				editor.entity().replace(insertedEntity);
				Set<Entity> inserted = singleton(insertedEntity);
				notifyAfterInsert(inserted);

				return inserted;
			}
		}
	}

	private final class InsertEntities implements EditTask {

		private final Collection<Entity> entities;

		private InsertEntities(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			settings.verifyInsertEnabled();
			editor.validate(entities);
		}

		@Override
		public Task prepare() {
			notifyBeforeInsert(entities);

			return new InsertTask();
		}

		private final class InsertTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - insert {}", DefaultEntityEditModel.this, entities);

				return new InsertResult(unmodifiableCollection(persistence.insert(entities, connection())));
			}
		}

		private final class InsertResult implements Result {

			private final Collection<Entity> insertedEntities;

			private InsertResult(Collection<Entity> insertedEntities) {
				this.insertedEntities = insertedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				notifyAfterInsert(insertedEntities);

				return insertedEntities;
			}
		}
	}

	private final class UpdateEntity implements EditTask {

		private final Entity entity = editor.entity().get().copy().mutable();

		private UpdateEntity() {
			settings.verifyUpdateEnabled(1);
			editor.validate(entity);
			verifyModified();
		}

		@Override
		public Task prepare() {
			notifyBeforeUpdate(singleton(entity));

			return new UpdateTask();
		}

		private void verifyModified() {
			if (!editor.modified().is()) {
				throw new IllegalStateException("Entity is not modified: " + editor.entity().get());
			}
		}

		private final class UpdateTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - update {}", DefaultEntityEditModel.this, entity);

				return new UpdateResult(persistence.update(entity, connection()));
			}
		}

		private final class UpdateResult implements Result {

			private final Entity updatedEntity;

			private UpdateResult(Entity updatedEntity) {
				this.updatedEntity = updatedEntity;
			}

			@Override
			public Collection<Entity> handle() {
				Entity editorEntity = editor.entity().get();
				editor.entity().replace(updatedEntity);
				notifyAfterUpdate(singletonMap(editorEntity, updatedEntity));

				return singleton(updatedEntity);
			}
		}
	}

	private final class UpdateEntities implements EditTask {

		private final Collection<Entity> entities;

		private UpdateEntities(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			settings.verifyUpdateEnabled(entities.size());
			editor.validate(entities);
			verifyModified(entities);
		}

		@Override
		public Task prepare() {
			notifyBeforeUpdate(entities);

			return new UpdateTask();
		}

		private void verifyModified(Collection<Entity> entities) {
			for (Entity entity : entities) {
				if (!entity.modified()) {
					throw new IllegalStateException("Entity is not modified: " + entity);
				}
			}
		}

		private final class UpdateTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - update {}", DefaultEntityEditModel.this, entities);

				return new UpdateResult(persistence.update(entities, connection()));
			}
		}

		private final class UpdateResult implements Result {

			private final Collection<Entity> updatedEntities;

			private UpdateResult(Collection<Entity> updatedEntities) {
				this.updatedEntities = updatedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				notifyAfterUpdate(originalEntityMap(entities, updatedEntities));

				return updatedEntities;
			}
		}
	}

	private final class DeleteEntity implements EditTask {

		private final Entity entity = activeEntity();

		private DeleteEntity() {
			settings.verifyDeleteEnabled();
		}

		@Override
		public Task prepare() {
			notifyBeforeDelete(singleton(entity));

			return new DeleteTask();
		}

		private Entity activeEntity() {
			Entity copy = editor.entity().get().copy().mutable();
			copy.revert();

			return copy;
		}

		private final class DeleteTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - delete {}", DefaultEntityEditModel.this, entity);
				persistence.delete(entity, connection());

				return new DeleteResult(entity);
			}
		}

		private final class DeleteResult implements Result {

			private final Entity deletedEntity;

			private DeleteResult(Entity deletedEntity) {
				this.deletedEntity = deletedEntity;
			}

			@Override
			public Collection<Entity> handle() {
				editor.defaults();
				Set<Entity> deleted = singleton(deletedEntity);
				notifyAfterDelete(deleted);

				return deleted;
			}
		}
	}

	private final class DeleteEntities implements EditTask {

		private final Collection<Entity> entities;

		private DeleteEntities(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			settings.verifyDeleteEnabled();
		}

		@Override
		public Task prepare() {
			notifyBeforeDelete(entities);

			return new DeleteTask();
		}

		private final class DeleteTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - delete {}", DefaultEntityEditModel.this, entities);
				persistence.delete(entities, connection());

				return new DeleteResult(entities);
			}
		}

		private final class DeleteResult implements Result {

			private final Collection<Entity> deletedEntities;

			private DeleteResult(Collection<Entity> deletedEntities) {
				this.deletedEntities = deletedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				notifyAfterDelete(deletedEntities);

				return deletedEntities;
			}
		}
	}

	private static final class Events {

		private final Event<Collection<Entity>> beforeInsert = Event.event();
		private final Event<Collection<Entity>> afterInsert = Event.event();
		private final Event<Collection<Entity>> beforeUpdate = Event.event();
		private final Event<Map<Entity, Entity>> afterUpdate = Event.event();
		private final Event<Collection<Entity>> beforeDelete = Event.event();
		private final Event<Collection<Entity>> afterDelete = Event.event();
		private final Event<Collection<Entity>> persisted = Event.event();

		private final ObservableState publishPersistenceEvents;

		private Events(ObservableState publishPersistenceEvents) {
			this.publishPersistenceEvents = publishPersistenceEvents;
		}

		private void notifyInserted(Collection<Entity> inserted) {
			afterInsert.accept(inserted);
			persisted.accept(inserted);
			if (publishPersistenceEvents.is()) {
				DefaultEntityEditModel.notifyInserted(inserted);
			}
		}

		private void notifyUpdated(Map<Entity, Entity> updated) {
			afterUpdate.accept(updated);
			persisted.accept(updated.values());
			if (publishPersistenceEvents.is()) {
				DefaultEntityEditModel.notifyUpdated(updated);
			}
		}

		private void notifyDeleted(Collection<Entity> deleted) {
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