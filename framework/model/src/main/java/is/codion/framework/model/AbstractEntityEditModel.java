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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

import static is.codion.framework.model.EntityEditEvents.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

	private final DefaultEntityEditor editor;
	private final Map<ForeignKey, EntitySearchModel> entitySearchModels = new HashMap<>();

	private final Events events;
	private final States states;

	/**
	 * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		EntityDefinition entityDefinition = requireNonNull(connectionProvider).entities().definition(requireNonNull(entityType));
		this.editor = new DefaultEntityEditor(entityDefinition, connectionProvider);
		this.states = new States(entityDefinition.readOnly());
		this.events = new Events(states.postEditEvents);
	}

	@Override
	public final Entities entities() {
		return editor.connectionProvider().entities();
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
	public final State postEditEvents() {
		return states.postEditEvents;
	}

	@Override
	public final State readOnly() {
		return states.readOnly;
	}

	@Override
	public final State insertEnabled() {
		return states.insertEnabled;
	}

	@Override
	public final State updateEnabled() {
		return states.updateEnabled;
	}

	@Override
	public final State updateMultipleEnabled() {
		return states.updateMultipleEnabled;
	}

	@Override
	public final State deleteEnabled() {
		return states.deleteEnabled;
	}

	@Override
	public final EntityType entityType() {
		return editor.entity().entityType();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editor.connectionProvider();
	}

	@Override
	public final EntityConnection connection() {
		return connectionProvider().connection();
	}

	@Override
	public final void replace(ForeignKey foreignKey, Collection<Entity> entities) {
		replaceForeignKey(requireNonNull(foreignKey), requireNonNull(entities));
	}

	@Override
	public final EntityEditor entity() {
		return editor;
	}

	@Override
	public final <T> ValueEditor<T> value(Attribute<T> attribute) {
		return editor.value(attribute);
	}

	@Override
	public final Entity insert() {
		return createInsert().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) {
		return createInsert(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() {
		return createUpdate().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) {
		return createUpdate(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() {
		return createDelete().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) {
		return createDelete(entities).prepare().perform().handle();
	}

	@Override
	public final Insert createInsert() {
		return new DefaultInsert();
	}

	@Override
	public final Insert createInsert(Collection<Entity> entities) {
		return new DefaultInsert(entities);
	}

	@Override
	public final Update createUpdate() {
		return new DefaultUpdate();
	}

	@Override
	public final Update createUpdate(Collection<Entity> entities) {
		return new DefaultUpdate(entities);
	}

	@Override
	public final Delete createDelete() {
		return new DefaultDelete();
	}

	@Override
	public final Delete createDelete(Collection<Entity> entities) {
		return new DefaultDelete(entities);
	}

	@Override
	public EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		Collection<Column<String>> searchable = entities().definition(foreignKey.referencedType()).columns().searchable();
		if (searchable.isEmpty()) {
			throw new IllegalStateException("No searchable columns defined for entity: " + foreignKey.referencedType());
		}

		return EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider())
						.columns(searchable)
						.singleSelection(true)
						.build();
	}

	@Override
	public final EntitySearchModel foreignKeySearchModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		synchronized (entitySearchModels) {
			// can't use computeIfAbsent here, see comment in SwingEntityEditModel.foreignKeyComboBoxModel()
			EntitySearchModel entitySearchModel = entitySearchModels.get(foreignKey);
			if (entitySearchModel == null) {
				entitySearchModel = createForeignKeySearchModel(foreignKey);
				entitySearchModels.put(foreignKey, entitySearchModel);
			}

			return entitySearchModel;
		}
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
	public final Observer<Map<Entity.Key, Entity>> beforeUpdate() {
		return events.beforeUpdate.observer();
	}

	@Override
	public final Observer<Map<Entity.Key, Entity>> afterUpdate() {
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
	public final Observer<?> afterInsertUpdateOrDelete() {
		return events.afterInsertUpdateOrDelete.observer();
	}

	/**
	 * Inserts the given entities into the database using the given connection
	 * @param entities the entities to insert
	 * @param connection the connection to use
	 * @return the inserted entities
	 * @throws DatabaseException in case of a database exception
	 */
	protected Collection<Entity> insert(Collection<Entity> entities, EntityConnection connection) {
		return requireNonNull(connection).insertSelect(entities);
	}

	/**
	 * Updates the given entities in the database using the given connection
	 * @param entities the entities to update
	 * @param connection the connection to use
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 */
	protected Collection<Entity> update(Collection<Entity> entities, EntityConnection connection) {
		return requireNonNull(connection).updateSelect(entities);
	}

	/**
	 * Deletes the given entities from the database using the given connection
	 * @param entities the entities to delete
	 * @param connection the connection to use
	 * @throws DatabaseException in case of a database exception
	 */
	protected void delete(Collection<Entity> entities, EntityConnection connection) {
		requireNonNull(connection).delete(Entity.primaryKeys(entities));
	}

	/**
	 * For every field referencing the given foreign key values, replaces that foreign key instance with
	 * the corresponding entity from {@code values}, useful when attribute
	 * values have been changed in the referenced entity that must be reflected in the edit model.
	 * @param foreignKey the foreign key attribute
	 * @param values the foreign key entities
	 */
	protected void replaceForeignKey(ForeignKey foreignKey, Collection<Entity> values) {
		Entity currentForeignKeyValue = editor.entity().entity(foreignKey);
		if (currentForeignKeyValue != null) {
			for (Entity replacementValue : values) {
				if (currentForeignKeyValue.equals(replacementValue)) {
					value(foreignKey).clear();
					value(foreignKey).set(replacementValue);
				}
			}
		}
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
		events.afterInsert.accept(requireNonNull(insertedEntities));
	}

	/**
	 * Notifies that update is about to be performed
	 * @param entitiesToUpdate the entities about to be updated
	 * @see #beforeUpdate()
	 */
	protected final void notifyBeforeUpdate(Map<Entity.Key, Entity> entitiesToUpdate) {
		events.beforeUpdate.accept(requireNonNull(entitiesToUpdate));
	}

	/**
	 * Notifies that update has been performed
	 * @param updatedEntities the updated entities
	 * @see #afterUpdate()
	 */
	protected final void notifyAfterUpdate(Map<Entity.Key, Entity> updatedEntities) {
		events.afterUpdate.accept(requireNonNull(updatedEntities));
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
		events.afterDelete.accept(requireNonNull(deletedEntities));
	}

	/**
	 * Maps the given entities and their updated counterparts to their original primary keys,
	 * assumes a single copy of each entity in the given lists.
	 * @param entitiesBeforeUpdate the entities before update
	 * @param entitiesAfterUpdate the entities after update
	 * @return the updated entities mapped to their respective original primary keys
	 */
	private static Map<Entity.Key, Entity> originalPrimaryKeyMap(Collection<Entity> entitiesBeforeUpdate,
																															 Collection<Entity> entitiesAfterUpdate) {
		List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
		Map<Entity.Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
		for (Entity entity : entitiesBeforeUpdate) {
			keyMap.put(entity.originalPrimaryKey(), findAndRemove(entity.primaryKey(), entitiesAfterUpdateCopy.listIterator()));
		}

		return unmodifiableMap(keyMap);
	}

	private static Entity findAndRemove(Entity.Key primaryKey, ListIterator<Entity> iterator) {
		while (iterator.hasNext()) {
			Entity current = iterator.next();
			if (current.primaryKey().equals(primaryKey)) {
				iterator.remove();

				return current;
			}
		}

		return null;
	}

	private final class DefaultInsert implements Insert {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultInsert() {
			this.entities = entityForInsert();
			this.activeEntity = true;
			states.verifyInsertEnabled();
			editor.validate(entities);
		}

		private DefaultInsert(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			this.activeEntity = false;
			states.verifyInsertEnabled();
			editor.validate(entities);
		}

		private Collection<Entity> entityForInsert() {
			Entity toInsert = editor.entity().copy().mutable();
			if (toInsert.definition().primaryKey().generated()) {
				toInsert.clearPrimaryKey();
			}

			return singleton(toInsert);
		}

		@Override
		public Task prepare() {
			notifyBeforeInsert(entities);

			return new InsertTask();
		}

		private final class InsertTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - insert {}", this, entities);
				Collection<Entity> inserted = unmodifiableCollection(insert(entities, connection()));
				if (!entities.isEmpty() && inserted.isEmpty()) {
					throw new DatabaseException("Insert did not return an entity, usually caused by a misconfigured key generator");
				}

				return new InsertResult(inserted);
			}
		}

		private final class InsertResult implements Result {

			private final Collection<Entity> insertedEntities;

			private InsertResult(Collection<Entity> insertedEntities) {
				this.insertedEntities = insertedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				if (activeEntity) {
					editor.setOrDefaults(insertedEntities.iterator().next());
				}
				notifyAfterInsert(insertedEntities);

				return insertedEntities;
			}
		}
	}

	private final class DefaultUpdate implements Update {

		private final Collection<Entity> entities;

		private DefaultUpdate() {
			entities = singleton(editor.entity().copy().mutable());
			states.verifyUpdateEnabled(entities.size());
			editor.validate(entities);
			verifyModified(entities);
		}

		private DefaultUpdate(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			states.verifyUpdateEnabled(entities.size());
			editor.validate(entities);
			verifyModified(entities);
		}

		@Override
		public Task prepare() {
			notifyBeforeUpdate(unmodifiableMap(entities.stream().collect(toMap(Entity::originalPrimaryKey, Function.identity()))));

			return new UpdateTask();
		}

		private void verifyModified(Collection<Entity> entities) {
			for (Entity entityToUpdate : entities) {
				if (!entityToUpdate.modified()) {
					throw new IllegalArgumentException("Entity is not modified: " + entityToUpdate);
				}
			}
		}

		private final class UpdateTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - update {}", this, entities);

				return new UpdateResult(update(entities, connection()));
			}
		}

		private final class UpdateResult implements Result {

			private final Collection<Entity> updatedEntities;

			private UpdateResult(Collection<Entity> updatedEntities) {
				this.updatedEntities = updatedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				Entity entity = editor.get();
				updatedEntities.stream()
								.filter(updatedEntity -> updatedEntity.equals(entity))
								.findFirst()
								.ifPresent(editor::setOrDefaults);
				notifyAfterUpdate(originalPrimaryKeyMap(entities, updatedEntities));

				return updatedEntities;
			}
		}
	}

	private final class DefaultDelete implements Delete {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultDelete() {
			this.entities = singleton(activeEntity());
			this.activeEntity = true;
			states.verifyDeleteEnabled();
		}

		private DefaultDelete(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			this.activeEntity = false;
			states.verifyDeleteEnabled();
		}

		@Override
		public Task prepare() {
			notifyBeforeDelete(entities);

			return new DeleteTask();
		}

		private Entity activeEntity() {
			Entity copy = editor.entity().copy().mutable();
			copy.revert();

			return copy;
		}

		private final class DeleteTask implements Task {

			@Override
			public Result perform() {
				LOG.debug("{} - delete {}", this, entities);
				delete(entities, connection());

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
				if (activeEntity) {
					editor.setOrDefaults(null);
				}
				notifyAfterDelete(deletedEntities);

				return deletedEntities;
			}
		}
	}

	private static final class Events {

		private final Event<Collection<Entity>> beforeInsert = Event.event();
		private final Event<Collection<Entity>> afterInsert = Event.event();
		private final Event<Map<Entity.Key, Entity>> beforeUpdate = Event.event();
		private final Event<Map<Entity.Key, Entity>> afterUpdate = Event.event();
		private final Event<Collection<Entity>> beforeDelete = Event.event();
		private final Event<Collection<Entity>> afterDelete = Event.event();
		private final Event<?> afterInsertUpdateOrDelete = Event.event();

		private Events(ObservableState postEditEvents) {
			afterInsert.addListener(afterInsertUpdateOrDelete);
			afterUpdate.addListener(afterInsertUpdateOrDelete);
			afterDelete.addListener(afterInsertUpdateOrDelete);
			afterInsert.addConsumer(insertedEntities -> {
				if (postEditEvents.get()) {
					inserted(insertedEntities);
				}
			});
			afterUpdate.addConsumer(updatedEntities -> {
				if (postEditEvents.get()) {
					updated(updatedEntities);
				}
			});
			afterDelete.addConsumer(deletedEntities -> {
				if (postEditEvents.get()) {
					deleted(deletedEntities);
				}
			});
		}
	}

	private static final class States {

		private final State readOnly;
		private final State insertEnabled = State.state(true);
		private final State updateEnabled = State.state(true);
		private final State updateMultipleEnabled = State.state(true);
		private final State deleteEnabled = State.state(true);
		private final State postEditEvents = State.state(POST_EDIT_EVENTS.getOrThrow());

		private States(boolean readOnly) {
			this.readOnly = State.state(readOnly);
		}

		private void verifyInsertEnabled() {
			if (readOnly.get() || !insertEnabled.get()) {
				throw new IllegalStateException("Edit model is readOnly or inserting is not enabled!");
			}
		}

		private void verifyUpdateEnabled(int entityCount) {
			if (readOnly.get() || !updateEnabled.get()) {
				throw new IllegalStateException("Edit model is readOnly or updating is not enabled!");
			}
			if (entityCount > 1 && !updateMultipleEnabled.get()) {
				throw new IllegalStateException("Updating multiple entities is not enabled");
			}
		}

		private void verifyDeleteEnabled() {
			if (readOnly.get() || !deleteEnabled.get()) {
				throw new IllegalStateException("Edit model is readOnly or deleting is not enabled!");
			}
		}
	}
}