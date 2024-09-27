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
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.model.EntityEditEvents.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

	private final DefaultEditableEntity editable;
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
		this.editable = new DefaultEditableEntity(entityDefinition, connectionProvider);
		this.states = new States(entityDefinition.readOnly());
		this.events = new Events(states.postEditEvents);
	}

	@Override
	public final Entities entities() {
		return editable.connectionProvider.entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return editable.entityDefinition;
	}

	@Override
	public final String toString() {
		return getClass() + ", " + editable.entity.entityType();
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
		return editable.entity.entityType();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editable.connectionProvider;
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
	public final EditableEntity entity() {
		return editable;
	}

	@Override
	public final <T> EditableValue<T> value(Attribute<T> attribute) {
		return editable.value(attribute);
	}

	@Override
	public final void validate(Attribute<?> attribute) throws ValidationException {
		editable.validator.get().validate(editable.entity, attribute);
	}

	@Override
	public final void validate(Collection<Entity> entities) throws ValidationException {
		for (Entity entityToValidate : requireNonNull(entities)) {
			validate(entityToValidate);
		}
	}

	@Override
	public final void validate(Entity entity) throws ValidationException {
		if (entity.entityType().equals(entityType())) {
			editable.validator.get().validate(entity);
		}
		else {
			entity.definition().validator().validate(entity);
		}
	}

	@Override
	public final Entity insert() throws DatabaseException, ValidationException {
		return createInsert().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) throws DatabaseException, ValidationException {
		return createInsert(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() throws DatabaseException, ValidationException {
		return createUpdate().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) throws DatabaseException, ValidationException {
		return createUpdate(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() throws DatabaseException {
		return createDelete().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) throws DatabaseException {
		return createDelete(entities).prepare().perform().handle();
	}

	@Override
	public final Insert createInsert() throws ValidationException {
		return new DefaultInsert();
	}

	@Override
	public final Insert createInsert(Collection<Entity> entities) throws ValidationException {
		return new DefaultInsert(entities);
	}

	@Override
	public final Update createUpdate() throws ValidationException {
		return new DefaultUpdate();
	}

	@Override
	public final Update createUpdate(Collection<Entity> entities) throws ValidationException {
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
	protected Collection<Entity> insert(Collection<Entity> entities, EntityConnection connection) throws DatabaseException {
		return requireNonNull(connection).insertSelect(entities);
	}

	/**
	 * Updates the given entities in the database using the given connection
	 * @param entities the entities to update
	 * @param connection the connection to use
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 */
	protected Collection<Entity> update(Collection<Entity> entities, EntityConnection connection) throws DatabaseException {
		return requireNonNull(connection).updateSelect(entities);
	}

	/**
	 * Deletes the given entities from the database using the given connection
	 * @param entities the entities to delete
	 * @param connection the connection to use
	 * @throws DatabaseException in case of a database exception
	 */
	protected void delete(Collection<Entity> entities, EntityConnection connection) throws DatabaseException {
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
		Entity currentForeignKeyValue = editable.entity.entity(foreignKey);
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

	private interface ValueSupplier {
		<T> T get(AttributeDefinition<T> attributeDefinition);
	}

	private final class DefaultInsert implements Insert {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultInsert() throws ValidationException {
			this.entities = entityForInsert();
			this.activeEntity = true;
			states.verifyInsertEnabled();
			validate(entities);
		}

		private DefaultInsert(Collection<Entity> entities) throws ValidationException {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			this.activeEntity = false;
			states.verifyInsertEnabled();
			validate(entities);
		}

		private Collection<Entity> entityForInsert() {
			Entity toInsert = editable.entity.copy();
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
			public Result perform() throws DatabaseException {
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
					editable.setOrDefaults(insertedEntities.iterator().next());
				}
				notifyAfterInsert(insertedEntities);

				return insertedEntities;
			}
		}
	}

	private final class DefaultUpdate implements Update {

		private final Collection<Entity> entities;

		private DefaultUpdate() throws ValidationException {
			entities = singleton(editable.entity.copy());
			states.verifyUpdateEnabled(entities.size());
			validate(entities);
			verifyModified(entities);
		}

		private DefaultUpdate(Collection<Entity> entities) throws ValidationException {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			states.verifyUpdateEnabled(entities.size());
			validate(entities);
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
			public Result perform() throws DatabaseException {
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
				Entity entity = editable.get();
				updatedEntities.stream()
								.filter(updatedEntity -> updatedEntity.equals(entity))
								.findFirst()
								.ifPresent(editable::setOrDefaults);
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
			Entity copy = editable.entity.copy();
			copy.revert();

			return copy;
		}

		private final class DeleteTask implements Task {

			@Override
			public Result perform() throws DatabaseException {
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
					editable.setOrDefaults(null);
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

		private Events(StateObserver postEditEvents) {
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
		private final State postEditEvents = State.state(POST_EDIT_EVENTS.get());

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

	private static final class DefaultEditableEntity implements EditableEntity {

		private final Map<Attribute<?>, Event<?>> editEvents = new HashMap<>();
		private final Event<Attribute<?>> valueChanged = Event.event();
		private final Event<Entity> changing = Event.event();
		private final Event<Entity> changed = Event.event();

		private final Map<Attribute<?>, DefaultEditableValue<?>> editableValues = new HashMap<>();
		private final Map<Attribute<?>, State> persistValues = new HashMap<>();
		private final Map<Attribute<?>, State> attributeModified = new HashMap<>();
		private final Map<Attribute<?>, State> attributeNull = new HashMap<>();
		private final Map<Attribute<?>, State> attributeValid = new HashMap<>();

		private final EntityDefinition entityDefinition;
		private final EntityConnectionProvider connectionProvider;
		private final State primaryKeyNull = State.state(true);
		private final State entityValid = State.state();
		private final DefaultExists exists;
		private final DefaultModified modified;
		private final StateObserver editing;
		private final Value<EntityValidator> validator;

		private final Entity entity;

		private DefaultEditableEntity(EntityDefinition entityDefinition, EntityConnectionProvider connectionProvider) {
			this.entityDefinition = entityDefinition;
			this.connectionProvider = connectionProvider;
			this.entity = createEntity(AttributeDefinition::defaultValue);
			this.exists = new DefaultExists(entity.definition());
			this.modified = new DefaultModified();
			this.editing = State.and(exists.exists, modified.modified);
			this.validator = Value.builder()
							.nonNull(entityDefinition.validator())
							.listener(this::updateValidState)
							.build();
			configurePersistentForeignKeys();
		}

		@Override
		public void set(Entity entity) {
			changing.accept(entity);
			setOrDefaults(entity);
		}

		@Override
		public Entity get() {
			return entity.immutable();
		}

		@Override
		public void clear() {
			set(entityDefinition.entity());
		}

		@Override
		public Observer<Entity> observer() {
			return changed.observer();
		}

		@Override
		public void defaults() {
			set(null);
		}

		@Override
		public void refresh() {
			try {
				if (exists.get()) {
					set(connectionProvider.connection().select(entity.primaryKey()));
				}
			}
			catch (DatabaseException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void revert() {
			entityDefinition.attributes().get().forEach(attribute -> value(attribute).revert());
		}

		@Override
		public Exists exists() {
			return exists;
		}

		@Override
		public Modified modified() {
			return modified;
		}

		@Override
		public StateObserver edited() {
			return editing;
		}

		@Override
		public Observer<Entity> changing() {
			return changing.observer();
		}

		@Override
		public Observer<Attribute<?>> valueChanged() {
			return valueChanged.observer();
		}

		@Override
		public boolean nullable(Attribute<?> attribute) {
			return validator.get().nullable(entity, attribute);
		}

		@Override
		public StateObserver isNull(Attribute<?> attribute) {
			return attributeNull.computeIfAbsent(attribute,
							k -> State.state(entity.isNull(attribute))).observer();
		}

		@Override
		public StateObserver isNotNull(Attribute<?> attribute) {
			return attributeNull.computeIfAbsent(attribute,
							k -> State.state(entity.isNull(attribute))).observer().not();
		}

		@Override
		public StateObserver primaryKeyNull() {
			return primaryKeyNull.observer();
		}

		@Override
		public Value<EntityValidator> validator() {
			return validator;
		}

		@Override
		public StateObserver valid() {
			return entityValid.observer();
		}

		@Override
		public void validate() throws ValidationException {
			validate(entity);
		}

		@Override
		public <T> EditableValue<T> value(Attribute<T> attribute) {
			entityDefinition.attributes().definition(attribute);

			return (EditableValue<T>) editableValues.computeIfAbsent(attribute, k -> new DefaultEditableValue<>(attribute));
		}

		private void setOrDefaults(Entity entity) {
			Map<Attribute<?>, Object> affectedAttributes = this.entity.set(entity == null ? createEntity(this::defaultValue) : entity);
			for (Attribute<?> affectedAttribute : affectedAttributes.keySet()) {
				notifyValueChange(affectedAttribute);
			}
			if (affectedAttributes.isEmpty()) {//otherwise notifyValueChange() triggers entity state updates
				updateStates();
			}
			attributeModified.forEach(this::updateAttributeModifiedState);

			changed.accept(entity);
		}

		private <T> T defaultValue(AttributeDefinition<T> attributeDefinition) {
			if (value(attributeDefinition.attribute()).persist().get()) {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					return (T) entity.entity((ForeignKey) attributeDefinition.attribute());
				}

				return entity.get(attributeDefinition.attribute());
			}

			return value(attributeDefinition.attribute()).defaultValue().get().get();
		}

		private <T> void notifyValueEdit(Attribute<T> attribute, T value, Map<Attribute<?>, Object> dependingValues) {
			notifyValueChange(attribute);
			Event<T> editEvent = (Event<T>) editEvents.get(attribute);
			if (editEvent != null) {
				editEvent.accept(value);
			}
			dependingValues.forEach((dependingAttribute, previousValue) -> {
				Object currentValue = entity.get(dependingAttribute);
				if (!Objects.equals(previousValue, currentValue)) {
					notifyValueEdit((Attribute<Object>) dependingAttribute, currentValue, emptyMap());
				}
			});
		}

		private void notifyValueChange(Attribute<?> attribute) {
			updateStates();
			updateAttributeStates(attribute);
			DefaultEditableValue<?> editModelValue = editableValues.get(attribute);
			if (editModelValue != null) {
				editModelValue.valueChanged();
			}
			valueChanged.accept(attribute);
		}

		private void updateStates() {
			exists.update();
			modified.update();
			updateValidState();
			updatePrimaryKeyNullState();
		}

		private <T> void updateAttributeStates(Attribute<T> attribute) {
			State nullState = attributeNull.get(attribute);
			if (nullState != null) {
				nullState.set(entity.isNull(attribute));
			}
			State validState = attributeValid.get(attribute);
			if (validState != null) {
				validState.set(isValid(attribute));
			}
			State modifiedState = attributeModified.get(attribute);
			if (modifiedState != null) {
				updateAttributeModifiedState(attribute, modifiedState);
			}
		}

		private void validate(Entity entity) throws ValidationException {
			if (entity.entityType().equals(entityDefinition.entityType())) {
				validator.get().validate(entity);
			}
			else {
				entity.definition().validator().validate(entity);
			}
		}

		private boolean isValid(Attribute<?> attribute) {
			try {
				validator.get().validate(entity, attribute);
				return true;
			}
			catch (ValidationException e) {
				return false;
			}
		}

		private void updateAttributeModifiedState(Attribute<?> attribute, State modifiedState) {
			modifiedState.set(exists.predicate.get().test(entity) && entity.modified(attribute));
		}

		private void updateValidState() {
			entityValid.set(validator.get().valid(entity));
		}

		private void updatePrimaryKeyNullState() {
			primaryKeyNull.set(entity.primaryKey().isNull());
		}

		/**
		 * Instantiates a new {@link Entity} using the values provided by {@code valueSupplier}.
		 * Values are populated for {@link ColumnDefinition} and its descendants, {@link ForeignKeyDefinition}
		 * and {@link TransientAttributeDefinition} (excluding its descendants).
		 * If a {@link ColumnDefinition}s underlying column has a default value the attribute is
		 * skipped unless the attribute itself has a default value, which then overrides the columns default value.
		 * @return an entity instance populated with default values
		 * @see ColumnDefinition.Builder#columnHasDefaultValue()
		 * @see ColumnDefinition.Builder#defaultValue(Object)
		 */
		private Entity createEntity(ValueSupplier valueSupplier) {
			Entity newEntity = entityDefinition.entity();
			addColumnValues(valueSupplier, newEntity);
			addTransientValues(valueSupplier, newEntity);
			addForeignKeyValues(valueSupplier, newEntity);

			newEntity.save();

			return newEntity;
		}

		private void addColumnValues(ValueSupplier valueSupplier, Entity newEntity) {
			entityDefinition.columns().definitions().stream()
							//these are set via their respective parent foreign key
							.filter(columnDefinition -> !entityDefinition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()))
							.filter(columnDefinition -> !columnDefinition.columnHasDefaultValue() || columnDefinition.hasDefaultValue())
							.map(columnDefinition -> (AttributeDefinition<Object>) columnDefinition)
							.forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
		}

		private void addTransientValues(ValueSupplier valueSupplier, Entity newEntity) {
			entityDefinition.attributes().definitions().stream()
							.filter(TransientAttributeDefinition.class::isInstance)
							.filter(attributeDefinition -> !attributeDefinition.derived())
							.map(attributeDefinition -> (AttributeDefinition<Object>) attributeDefinition)
							.forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
		}

		private void addForeignKeyValues(ValueSupplier valueSupplier, Entity newEntity) {
			entityDefinition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
							newEntity.put(foreignKeyDefinition.attribute(), valueSupplier.get(foreignKeyDefinition)));
		}

		private void configurePersistentForeignKeys() {
			if (PERSIST_FOREIGN_KEYS.get()) {
				entityDefinition.foreignKeys().get().forEach(foreignKey ->
								value(foreignKey).persist().set(foreignKeyWritable(foreignKey)));
			}
		}

		private boolean foreignKeyWritable(ForeignKey foreignKey) {
			return foreignKey.references().stream()
							.map(ForeignKey.Reference::column)
							.map(entityDefinition.columns()::definition)
							.filter(ColumnDefinition.class::isInstance)
							.map(ColumnDefinition.class::cast)
							.anyMatch(columnDefinition -> !columnDefinition.readOnly());
		}

		private final class DefaultExists implements Exists {

			private final State exists = State.state(false);
			private final Value<Predicate<Entity>> predicate;

			private DefaultExists(EntityDefinition definition) {
				predicate = Value.builder()
								.nonNull(definition.exists())
								.listener(this::update)
								.build();
			}

			@Override
			public Value<Predicate<Entity>> predicate() {
				return predicate;
			}

			@Override
			public StateObserver not() {
				return exists.not();
			}

			@Override
			public Boolean get() {
				return exists.get();
			}

			@Override
			public boolean addListener(Runnable listener) {
				return exists.addListener(listener);
			}

			@Override
			public boolean removeListener(Runnable listener) {
				return exists.removeListener(listener);
			}

			@Override
			public boolean addConsumer(Consumer<? super Boolean> consumer) {
				return exists.addConsumer(consumer);
			}

			@Override
			public boolean removeConsumer(Consumer<? super Boolean> consumer) {
				return exists.removeConsumer(consumer);
			}

			@Override
			public boolean addWeakListener(Runnable listener) {
				return exists.addWeakListener(listener);
			}

			@Override
			public boolean removeWeakListener(Runnable listener) {
				return exists.removeWeakListener(listener);
			}

			@Override
			public boolean addWeakConsumer(Consumer<? super Boolean> consumer) {
				return exists.addWeakConsumer(consumer);
			}

			@Override
			public boolean removeWeakConsumer(Consumer<? super Boolean> consumer) {
				return exists.removeWeakConsumer(consumer);
			}

			private void update() {
				exists.set(predicate.get().test(entity));
			}
		}

		private final class DefaultModified implements Modified {

			private final State modified = State.state();
			private final Value<Predicate<Entity>> predicate = Value.builder()
							.nonNull((Predicate<Entity>) Entity::modified)
							.listener(this::update)
							.build();

			@Override
			public Value<Predicate<Entity>> predicate() {
				return predicate;
			}

			@Override
			public StateObserver not() {
				return modified.not();
			}

			@Override
			public Boolean get() {
				return modified.get();
			}

			@Override
			public void update() {
				modified.set(predicate.get().test(entity));
			}

			@Override
			public boolean addListener(Runnable listener) {
				return modified.addListener(listener);
			}

			@Override
			public boolean removeListener(Runnable listener) {
				return modified.removeListener(listener);
			}

			@Override
			public boolean addConsumer(Consumer<? super Boolean> consumer) {
				return modified.addConsumer(consumer);
			}

			@Override
			public boolean removeConsumer(Consumer<? super Boolean> consumer) {
				return modified.removeConsumer(consumer);
			}

			@Override
			public boolean addWeakListener(Runnable listener) {
				return modified.addWeakListener(listener);
			}

			@Override
			public boolean removeWeakListener(Runnable listener) {
				return modified.removeWeakListener(listener);
			}

			@Override
			public boolean addWeakConsumer(Consumer<? super Boolean> consumer) {
				return modified.addWeakConsumer(consumer);
			}

			@Override
			public boolean removeWeakConsumer(Consumer<? super Boolean> consumer) {
				return modified.removeWeakConsumer(consumer);
			}
		}

		private final class DefaultEditableValue<T> extends AbstractValue<T> implements EditableValue<T> {

			private final Attribute<T> attribute;
			private final Value<Supplier<T>> defaultValue;

			private DefaultEditableValue(Attribute<T> attribute) {
				this.attribute = attribute;
				this.defaultValue = Value.builder()
									.nonNull((Supplier<T>) entityDefinition.attributes().definition(attribute)::defaultValue)
									.build();
			}

			@Override
			public void revert() {
				if (modified().get()) {
					super.set(entity.original(attribute));
				}
			}


			@Override
			public State persist() {
				return persistValues.computeIfAbsent(attribute, k -> State.state());
			}

			@Override
			public StateObserver valid() {
				return attributeValid.computeIfAbsent(attribute,
								k -> State.state(isValid(attribute))).observer();
			}

			@Override
			public StateObserver modified() {
				return attributeModified.computeIfAbsent(attribute,
								k -> State.state(exists.get() && entity.modified(attribute))).observer();
			}

			@Override
			public Observer<T> edited() {
				return ((Event<T>) editEvents.computeIfAbsent(attribute, k -> Event.event())).observer();
			}

			@Override
			public Value<Supplier<T>> defaultValue() {
				return defaultValue;
			}

			@Override
			protected T getValue() {
				return entity.get(attribute);
			}

			@Override
			protected void setValue(T value) {
				Map<Attribute<?>, Object> dependingValues = dependingValues(attribute);
				T previousValue = entity.put(attribute, value);
				if (!Objects.equals(value, previousValue)) {
					notifyValueEdit(attribute, value, dependingValues);
				}
			}

			private Map<Attribute<?>, Object> dependingValues(Attribute<?> attribute) {
				return dependingValues(attribute, new LinkedHashMap<>());
			}

			private Map<Attribute<?>, Object> dependingValues(Attribute<?> attribute, Map<Attribute<?>, Object> dependingValues) {
				addDependingDerivedAttributes(attribute, dependingValues);
				if (attribute instanceof Column) {
					addDependingForeignKeys((Column<?>) attribute, dependingValues);
				}
				else if (attribute instanceof ForeignKey) {
					addDependingReferencedColumns((ForeignKey) attribute, dependingValues);
				}

				return dependingValues;
			}

			private void addDependingDerivedAttributes(Attribute<?> attribute, Map<Attribute<?>, Object> dependingValues) {
				entityDefinition.attributes().derivedFrom(attribute).forEach(derivedAttribute -> {
					dependingValues.put(derivedAttribute, entity.get(derivedAttribute));
					addDependingDerivedAttributes(derivedAttribute, dependingValues);
				});
			}

			private void addDependingForeignKeys(Column<?> column, Map<Attribute<?>, Object> dependingValues) {
				entityDefinition.foreignKeys().definitions(column).forEach(foreignKeyDefinition ->
								dependingValues.put(foreignKeyDefinition.attribute(), entity.get(foreignKeyDefinition.attribute())));
			}

			private void addDependingReferencedColumns(ForeignKey foreignKey, Map<Attribute<?>, Object> dependingValues) {
				foreignKey.references().forEach(reference ->
								dependingValues.put(reference.column(), entity.get(reference.column())));
			}

			private void valueChanged() {
				notifyListeners();
			}
		}
	}
}