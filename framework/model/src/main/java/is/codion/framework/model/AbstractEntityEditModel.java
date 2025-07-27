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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.model.EntityEditModel.events;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

	static final EditEvents EVENTS = new DefaultEditEvents();

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final DefaultEntityEditor editor;
	private final Map<ForeignKey, EntitySearchModel> searchModels = new HashMap<>();

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Collection<Entity>> insertListener = new InsertListener();
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private final Events events;
	private final States states;

	/**
	 * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this.entityDefinition = requireNonNull(connectionProvider).entities().definition(requireNonNull(entityType));
		this.connectionProvider = connectionProvider;
		this.editor = new DefaultEntityEditor(entityDefinition);
		this.states = new States(entityDefinition.readOnly());
		this.events = new Events();
		addEditListeners();
	}

	@Override
	public final Entities entities() {
		return connectionProvider.entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return entityDefinition;
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entityType();
	}

	@Override
	public final State editEvents() {
		return states.editEvents;
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
		return entityDefinition.type();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public final EntityConnection connection() {
		return connectionProvider().connection();
	}

	@Override
	public final EntityEditor editor() {
		return editor;
	}

	@Override
	public final void refresh() {
		if (editor.exists().get()) {
			editor.set(connectionProvider.connection().select(editor.getOrThrow().originalPrimaryKey()));
		}
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
	public final InsertEntities createInsert() {
		return new DefaultInsertEntities();
	}

	@Override
	public final InsertEntities createInsert(Collection<Entity> entities) {
		return new DefaultInsertEntities(entities);
	}

	@Override
	public final UpdateEntities createUpdate() {
		return new DefaultUpdateEntities();
	}

	@Override
	public final UpdateEntities createUpdate(Collection<Entity> entities) {
		return new DefaultUpdateEntities(entities);
	}

	@Override
	public final DeleteEntities createDelete() {
		return new DefaultDeleteEntities();
	}

	@Override
	public final DeleteEntities createDelete(Collection<Entity> entities) {
		return new DefaultDeleteEntities(entities);
	}

	@Override
	public EntitySearchModel createSearchModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		Collection<Column<String>> searchable = entities().definition(foreignKey.referencedType()).columns().searchable();
		if (searchable.isEmpty()) {
			throw new IllegalStateException("No searchable columns defined for entity: " + foreignKey.referencedType());
		}

		return EntitySearchModel.builder()
						.entityType(foreignKey.referencedType())
						.connectionProvider(connectionProvider())
						.build();
	}

	@Override
	public final EntitySearchModel searchModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		synchronized (searchModels) {
			// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
			// box models, createSearchModel() may for example call this function
			// see javadoc: must not attempt to update any other mappings of this map
			EntitySearchModel entitySearchModel = searchModels.get(foreignKey);
			if (entitySearchModel == null) {
				entitySearchModel = createSearchModel(foreignKey);
				configureSearchModel(foreignKey, entitySearchModel);
				searchModels.put(foreignKey, entitySearchModel);
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
	public final Observer<?> afterInsertUpdateOrDelete() {
		return events.afterInsertUpdateOrDelete.observer();
	}

	@Override
	public <T> void applyEdit(Collection<Entity> entities, Attribute<T> attribute, @Nullable T value) {
		requireNonNull(attribute);
		requireNonNull(entities).forEach(entity -> entity.set(attribute, value));
	}

	/**
	 * <p>Called when a {@link EntitySearchModel} is created in {@link #searchModel(ForeignKey)}.
	 * @param foreignKey the foreign key
	 * @param entitySearchModel the search model
	 */
	protected void configureSearchModel(ForeignKey foreignKey, EntitySearchModel entitySearchModel) {}

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
	 * <p>Called when entities of the type referenced by the given foreign key are inserted.
	 * @param foreignKey the foreign key
	 * @param entities the inserted entities
	 * @see EditEvents#inserted(EntityType)
	 */
	protected void inserted(ForeignKey foreignKey, Collection<Entity> entities) {}

	/**
	 * <p>Called when entities of the type referenced by the given foreign key have been updated.
	 * <p>For every field referencing the given foreign key values, replaces that foreign key instance with
	 * the corresponding value from {@code entities}
	 * @param foreignKey the foreign key
	 * @param entities the updated entities, mapped to their original primary key
	 * @see EditEvents#updated(EntityType)
	 */
	protected void updated(ForeignKey foreignKey, Map<Entity.Key, Entity> entities) {
		requireNonNull(foreignKey);
		requireNonNull(entities);
		Entity currentForeignKeyValue = editor.value(foreignKey).get();
		if (currentForeignKeyValue != null && entities.containsKey(currentForeignKeyValue.primaryKey())) {
			editor.value(foreignKey).clear();
			editor.value(foreignKey).set(entities.get(currentForeignKeyValue.primaryKey()));
		}
	}

	/**
	 * <p>Called when entities of the type referenced by the given foreign key are deleted.
	 * <p>Clears any foreign key values referencing the deleted entities.
	 * @param foreignKey the foreign key
	 * @param entities the deleted entities
	 * @see EditEvents#deleted(EntityType)
	 */
	protected void deleted(ForeignKey foreignKey, Collection<Entity> entities) {
		requireNonNull(foreignKey);
		requireNonNull(entities);
		Entity currentForeignKeyValue = editor.value(foreignKey).get();
		if (currentForeignKeyValue != null && entities.contains(currentForeignKeyValue)) {
			editor.value(foreignKey).clear();
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
	protected final void notifyBeforeUpdate(Collection<Entity> entitiesToUpdate) {
		events.beforeUpdate.accept(requireNonNull(entitiesToUpdate));
	}

	/**
	 * Notifies that update has been performed
	 * @param updatedEntities a map containing the updated entities, mapped to their state before the update
	 * @see #afterUpdate()
	 */
	protected final void notifyAfterUpdate(Map<Entity, Entity> updatedEntities) {
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
	 * Maps the given entities and their updated counterparts, assumes a single copy of each entity in the given lists.
	 * @param entitiesBeforeUpdate the entities before update
	 * @param entitiesAfterUpdate the entities after update
	 * @return the updated entities mapped to their respective state before the update
	 */
	private static Map<Entity, Entity> originalEntityMap(Collection<Entity> entitiesBeforeUpdate,
																											 Collection<Entity> entitiesAfterUpdate) {
		List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
		Map<Entity, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
		for (Entity entity : entitiesBeforeUpdate) {
			keyMap.put(entity.immutable(), findAndRemove(entity.primaryKey(), entitiesAfterUpdateCopy.listIterator()));
		}

		return unmodifiableMap(keyMap);
	}

	private static @Nullable Entity findAndRemove(Entity.Key primaryKey, ListIterator<Entity> iterator) {
		while (iterator.hasNext()) {
			Entity current = iterator.next();
			if (current.primaryKey().equals(primaryKey)) {
				iterator.remove();

				return current;
			}
		}

		return null;
	}

	private void addEditListeners() {
		entityDefinition.foreignKeys().get().stream()
						.map(ForeignKey::referencedType)
						.distinct()
						.forEach(entityType -> {
							events().inserted(entityType).addWeakConsumer(insertListener);
							events().updated(entityType).addWeakConsumer(updateListener);
							events().deleted(entityType).addWeakConsumer(deleteListener);
						});
	}

	private final class DefaultInsertEntities implements InsertEntities {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultInsertEntities() {
			this.entities = entityForInsert();
			this.activeEntity = true;
			states.verifyInsertEnabled();
			editor.validate(entities);
		}

		private DefaultInsertEntities(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			this.activeEntity = false;
			states.verifyInsertEnabled();
			editor.validate(entities);
		}

		private Collection<Entity> entityForInsert() {
			Entity.Builder builder = editor.getOrThrow().copy().builder();
			if (entityDefinition.primaryKey().generated()) {
				builder.clearPrimaryKey();
			}

			return singleton(builder.build());
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

	private final class DefaultUpdateEntities implements UpdateEntities {

		private final Collection<Entity> entities;

		private DefaultUpdateEntities() {
			entities = singleton(editor.getOrThrow().copy().mutable());
			states.verifyUpdateEnabled(entities.size());
			editor.validate(entities);
			verifyModified(entities);
		}

		private DefaultUpdateEntities(Collection<Entity> entities) {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			states.verifyUpdateEnabled(entities.size());
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
				notifyAfterUpdate(originalEntityMap(entities, updatedEntities));

				return updatedEntities;
			}
		}
	}

	private final class DefaultDeleteEntities implements DeleteEntities {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultDeleteEntities() {
			this.entities = singleton(activeEntity());
			this.activeEntity = true;
			states.verifyDeleteEnabled();
		}

		private DefaultDeleteEntities(Collection<Entity> entities) {
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
			Entity copy = editor.getOrThrow().copy().mutable();
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

	private final class Events {

		private final Event<Collection<Entity>> beforeInsert = Event.event();
		private final Event<Collection<Entity>> afterInsert = Event.event();
		private final Event<Collection<Entity>> beforeUpdate = Event.event();
		private final Event<Map<Entity, Entity>> afterUpdate = Event.event();
		private final Event<Collection<Entity>> beforeDelete = Event.event();
		private final Event<Collection<Entity>> afterDelete = Event.event();
		private final Event<?> afterInsertUpdateOrDelete = Event.event();

		private Events() {
			afterInsert.addListener(afterInsertUpdateOrDelete);
			afterUpdate.addListener(afterInsertUpdateOrDelete);
			afterDelete.addListener(afterInsertUpdateOrDelete);
			afterInsert.addConsumer(this::notifyInserted);
			afterUpdate.addConsumer(this::notifyUpdated);
			afterDelete.addConsumer(this::notifyDeleted);
		}

		private void notifyInserted(Collection<Entity> insertedEntities) {
			if (states.editEvents.get()) {
				groupByType(insertedEntities).forEach((entityType, entities) ->
								events().inserted(entityType).accept(entities));
			}
		}

		private void notifyUpdated(Map<Entity, Entity> updatedEntities) {
			if (states.editEvents.get()) {
				updatedEntities.entrySet()
								.stream()
								.collect(groupingBy(entry -> entry.getKey().type(), LinkedHashMap::new,
												toMap(Map.Entry::getKey, Map.Entry::getValue)))
								.forEach((entityType, entities) ->
												events().updated(entityType).accept(entities));
			}
		}

		private void notifyDeleted(Collection<Entity> deletedEntities) {
			if (states.editEvents.get()) {
				groupByType(deletedEntities).forEach((entityType, entities) ->
								events().deleted(entityType).accept(entities));
			}
		}
	}

	private static final class States {

		private final State readOnly;
		private final State insertEnabled = State.state(true);
		private final State updateEnabled = State.state(true);
		private final State updateMultipleEnabled = State.state(true);
		private final State deleteEnabled = State.state(true);
		private final State editEvents = State.state(EDIT_EVENTS.getOrThrow());

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

	private final class InsertListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> inserted) {
			onInsert(groupByType(inserted));
		}

		private void onInsert(Map<EntityType, List<Entity>> inserted) {
			inserted.forEach((entitType, value) ->
							entityDefinition.foreignKeys().get(entitType)
											.forEach(foreignKey -> inserted(foreignKey, value)));
		}
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			onDelete(groupByType(deleted));
		}

		private void onDelete(Map<EntityType, List<Entity>> deleted) {
			deleted.forEach((key, value) ->
							entityDefinition.foreignKeys().get(key)
											.forEach(foreignKey -> deleted(foreignKey, value)));
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Map<EntityType, Map<Entity.Key, Entity>> grouped = new HashMap<>();
			updated.forEach((beforeUpdate, afterUpdate) ->
							grouped.computeIfAbsent(beforeUpdate.type(), k -> new HashMap<>())
											.put(beforeUpdate.originalPrimaryKey(), afterUpdate));
			onUpdate(grouped);
		}

		private void onUpdate(Map<EntityType, Map<Entity.Key, Entity>> updated) {
			updated.forEach((entityType, entities) ->
							entityDefinition.foreignKeys().get(entityType)
											.forEach(foreignKey -> updated(foreignKey, entities)));
		}
	}

	private static final class DefaultEditEvents implements EditEvents {

		private final Map<EntityType, EditEvent<Collection<Entity>>> insertEvents = new ConcurrentHashMap<>();
		private final Map<EntityType, EditEvent<Map<Entity, Entity>>> updateEvents = new ConcurrentHashMap<>();
		private final Map<EntityType, EditEvent<Collection<Entity>>> deleteEvents = new ConcurrentHashMap<>();

		@Override
		public EditEvent<Collection<Entity>> inserted(EntityType entityType) {
			return insertEvents.computeIfAbsent(entityType, k -> new DefaultEntityEditEvent<>());
		}

		@Override
		public EditEvent<Map<Entity, Entity>> updated(EntityType entityType) {
			return updateEvents.computeIfAbsent(entityType, k -> new DefaultEntityEditEvent<>());
		}

		@Override
		public EditEvent<Collection<Entity>> deleted(EntityType entityType) {
			return deleteEvents.computeIfAbsent(entityType, k -> new DefaultEntityEditEvent<>());
		}
	}

	private static final class DefaultEntityEditEvent<T> implements EditEvent<T> {

		private final Event<T> event = Event.event();

		@Override
		public boolean addListener(Runnable listener) {
			return event.addListener(listener);
		}

		@Override
		public boolean removeListener(Runnable listener) {
			return event.removeListener(listener);
		}

		@Override
		public boolean addConsumer(Consumer<? super T> consumer) {
			return event.addConsumer(consumer);
		}

		@Override
		public boolean removeConsumer(Consumer<? super T> consumer) {
			return event.removeConsumer(consumer);
		}

		@Override
		public boolean addWeakListener(Runnable listener) {
			return event.addWeakListener(listener);
		}

		@Override
		public boolean removeWeakListener(Runnable listener) {
			return event.removeWeakListener(listener);
		}

		@Override
		public boolean addWeakConsumer(Consumer<? super T> consumer) {
			return event.addWeakConsumer(consumer);
		}

		@Override
		public boolean removeWeakConsumer(Consumer<? super T> consumer) {
			return event.removeWeakConsumer(consumer);
		}

		@Override
		public void accept(T entities) {
			event.accept(requireNonNull(entities));
		}
	}
}