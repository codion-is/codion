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
import is.codion.common.event.EventObserver;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

	private final Entity entity;
	private final EntityConnectionProvider connectionProvider;
	private final Map<ForeignKey, EntitySearchModel> entitySearchModels = new HashMap<>();
	private final Map<Attribute<?>, EditModelValue<?>> editModelValues = new ConcurrentHashMap<>();
	private final Map<Attribute<?>, State> persistValues = new ConcurrentHashMap<>();
	private final Map<Attribute<?>, Value<Supplier<?>>> defaultValues = new ConcurrentHashMap<>();
	private final Value<EntityValidator> validator;
	private final Value<Predicate<Entity>> modifiedPredicate;
	private final Value<Predicate<Entity>> existsPredicate;

	private final Events events = new Events();
	private final States states = new States();

	/**
	 * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
	 * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
	 * @param connectionProvider the {@link EntityConnectionProvider} instance
	 */
	protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this.entity = requireNonNull(connectionProvider).entities().entity(entityType);
		this.connectionProvider = connectionProvider;
		this.validator = Value.builder()
						.nonNull(entityDefinition().validator())
						.build();
		this.modifiedPredicate = Value.builder()
						.nonNull((Predicate<Entity>) Entity::modified)
						.build();
		this.existsPredicate = Value.builder()
						.nonNull(entityDefinition().exists())
						.build();
		this.states.readOnly.set(entityDefinition().readOnly());
		this.events.bindEvents();
		configurePersistentForeignKeys();
		setEntity(createEntity(AttributeDefinition::defaultValue));
	}

	@Override
	public final Entities entities() {
		return connectionProvider.entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return entity.definition();
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entity.entityType();
	}

	@Override
	public final <S extends Supplier<T>, T> Value<S> defaultValue(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition().attributes().definition(attribute);
		if (defaultValues.containsKey(attribute)) {
			return (Value<S>) defaultValues.get(attribute);
		}

		defaultValues.put(attribute, (Value<Supplier<?>>) Value.builder()
						.nonNull((S) (Supplier<T>) attributeDefinition::defaultValue)
						.build());

		return (Value<S>) defaultValues.get(attribute);
	}

	@Override
	public final State postEditEvents() {
		return states.postEditEvents;
	}

	@Override
	public final State persist(Attribute<?> attribute) {
		entityDefinition().attributes().definition(attribute);
		return persistValues.computeIfAbsent(attribute, k -> State.state());
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
	public final StateObserver exists() {
		return states.entityExists.observer();
	}

	@Override
	public final StateObserver editing() {
		return states.editing;
	}

	@Override
	public final StateObserver primaryKeyNull() {
		return states.primaryKeyNull.observer();
	}

	@Override
	public final void set(Entity entity) {
		events.entityChanging.accept(entity);
		setEntity(entity);
	}

	@Override
	public final void defaults() {
		setEntity(null);
	}

	@Override
	public final EntityType entityType() {
		return entity.entityType();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public final EntityConnection connection() {
		return connectionProvider.connection();
	}

	@Override
	public final void replace(ForeignKey foreignKey, Collection<Entity> entities) {
		replaceForeignKey(requireNonNull(foreignKey), requireNonNull(entities));
	}

	@Override
	public final Entity entity() {
		return entity.immutable();
	}

	@Override
	public final StateObserver modified() {
		return states.entityModified.observer();
	}

	@Override
	public final StateObserver modified(Attribute<?> attribute) {
		return states.modifiedObserver(attribute);
	}

	@Override
	public final void revert() {
		entityDefinition().attributes().get().forEach(this::revert);
	}

	@Override
	public final <T> void revert(Attribute<T> attribute) {
		if (modified(attribute).get()) {
			value(attribute).set(entity.original(attribute));
		}
	}

	@Override
	public final boolean nullable(Attribute<?> attribute) {
		return validator.get().nullable(entity, attribute);
	}

	@Override
	public final StateObserver isNull(Attribute<?> attribute) {
		return states.nullObserver(attribute);
	}

	@Override
	public final StateObserver isNotNull(Attribute<?> attribute) {
		return states.nullObserver(attribute).not();
	}

	@Override
	public final StateObserver valid() {
		return states.entityValid.observer();
	}

	@Override
	public final StateObserver valid(Attribute<?> attribute) {
		return states.validObserver(attribute);
	}

	@Override
	public final void validate(Attribute<?> attribute) throws ValidationException {
		validator.get().validate(entity, attribute);
	}

	@Override
	public final void validate() throws ValidationException {
		validate(entity);
	}

	@Override
	public final void validate(Collection<Entity> entities) throws ValidationException {
		for (Entity entityToValidate : requireNonNull(entities)) {
			validate(entityToValidate);
		}
	}

	@Override
	public void validate(Entity entity) throws ValidationException {
		if (entity.entityType().equals(entityType())) {
			validator.get().validate(entity);
		}
		else {
			entity.definition().validator().validate(entity);
		}
	}

	@Override
	public final Entity insert() throws DatabaseException, ValidationException {
		return new DefaultInsert().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) throws DatabaseException, ValidationException {
		return new DefaultInsert(entities).prepare().perform().handle();
	}

	@Override
	public final Entity update() throws DatabaseException, ValidationException {
		return new DefaultUpdate().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) throws DatabaseException, ValidationException {
		return new DefaultUpdate(entities).prepare().perform().handle();
	}

	@Override
	public final Entity delete() throws DatabaseException {
		return new DefaultDelete().prepare().perform().handle().iterator().next();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) throws DatabaseException {
		return new DefaultDelete(entities).prepare().perform().handle();
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
	public final void refresh() {
		try {
			if (states.entityExists.get()) {
				set(connection().select(entity.primaryKey()));
			}
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey) {
		entityDefinition().foreignKeys().definition(foreignKey);
		Collection<Column<String>> searchable = entities().definition(foreignKey.referencedType()).columns().searchable();
		if (searchable.isEmpty()) {
			throw new IllegalStateException("No searchable columns defined for entity: " + foreignKey.referencedType());
		}

		return EntitySearchModel.builder(foreignKey.referencedType(), connectionProvider)
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
	public final <T> Value<T> value(Attribute<T> attribute) {
		entityDefinition().attributes().definition(attribute);
		return (Value<T>) editModelValues.computeIfAbsent(attribute, k -> new EditModelValue<>(attribute));
	}

	@Override
	public final <T> EventObserver<T> valueEdited(Attribute<T> attribute) {
		return events.valueEdited(attribute);
	}

	@Override
	public final EventObserver<Attribute<?>> valueChanged() {
		return events.valueChange.observer();
	}

	@Override
	public final EventObserver<Entity> entityChanged() {
		return events.entityChanged.observer();
	}

	@Override
	public final EventObserver<Entity> entityChanging() {
		return events.entityChanging.observer();
	}

	@Override
	public final EventObserver<Collection<Entity>> beforeInsert() {
		return events.beforeInsert.observer();
	}

	@Override
	public final EventObserver<Collection<Entity>> afterInsert() {
		return events.afterInsert.observer();
	}

	@Override
	public final EventObserver<Map<Entity.Key, Entity>> beforeUpdate() {
		return events.beforeUpdate.observer();
	}

	@Override
	public final EventObserver<Map<Entity.Key, Entity>> afterUpdate() {
		return events.afterUpdate.observer();
	}

	@Override
	public final EventObserver<Collection<Entity>> beforeDelete() {
		return events.beforeDelete.observer();
	}

	@Override
	public final EventObserver<Collection<Entity>> afterDelete() {
		return events.afterDelete.observer();
	}

	@Override
	public final EventObserver<?> afterInsertUpdateOrDelete() {
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
		Entity currentForeignKeyValue = entity.entity(foreignKey);
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
	 * Controls the validator used by this edit model.
	 * @return the {@link Value} controlling the validator
	 * @see #validate(Entity)
	 */
	protected final Value<EntityValidator> validator() {
		return validator;
	}

	/**
	 * Controls the 'modified' predicate for this edit model, which is responsible for providing
	 * the modified state of the underlying entity.
	 * @return the {@link Value} controlling the predicate used to check if the entity is modified
	 * @see Entity#modified()
	 * @see #modified()
	 */
	protected final Value<Predicate<Entity>> modifiedPredicate() {
		return modifiedPredicate;
	}

	/**
	 * Controls the 'exists' predicate for this edit model, which is responsible for providing
	 * the exists state of the underlying entity.
	 * @return the {@link Value} controlling the predicate used to check if the entity exists
	 * @see EntityDefinition#exists()
	 * @see Entity#exists()
	 */
	protected final Value<Predicate<Entity>> existsPredicate() {
		return existsPredicate;
	}

	/**
	 * Updates the modified state.
	 * @see #modified()
	 */
	protected final void updateModifiedState() {
		states.updateModifiedState();
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

	private void setEntity(Entity entity) {
		Map<Attribute<?>, Object> affectedAttributes = this.entity.set(entity == null ? createEntity(this::defaultValue) : entity);
		for (Attribute<?> affectedAttribute : affectedAttributes.keySet()) {
			events.notifyValueChange(affectedAttribute);
		}
		if (affectedAttributes.isEmpty()) {//otherwise notifyValueChange() triggers entity state updates
			states.updateEntityStates();
		}
		states.updateAttributeModifiedStates();

		events.entityChanged.accept(entity);
	}

	private void configurePersistentForeignKeys() {
		if (EntityEditModel.PERSIST_FOREIGN_KEYS.get()) {
			entityDefinition().foreignKeys().get().forEach(foreignKey ->
							persist(foreignKey).set(foreignKeyWritable(foreignKey)));
		}
	}

	private boolean foreignKeyWritable(ForeignKey foreignKey) {
		return foreignKey.references().stream()
						.map(ForeignKey.Reference::column)
						.map(entityDefinition().columns()::definition)
						.filter(ColumnDefinition.class::isInstance)
						.map(ColumnDefinition.class::cast)
						.anyMatch(columnDefinition -> !columnDefinition.readOnly());
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
		EntityDefinition definition = entityDefinition();
		Entity newEntity = definition.entity();
		addColumnValues(valueSupplier, definition, newEntity);
		addTransientValues(valueSupplier, definition, newEntity);
		addForeignKeyValues(valueSupplier, definition, newEntity);

		newEntity.save();

		return newEntity;
	}

	private <T> T defaultValue(AttributeDefinition<T> attributeDefinition) {
		if (persist(attributeDefinition.attribute()).get()) {
			if (attributeDefinition instanceof ForeignKeyDefinition) {
				return (T) entity.entity((ForeignKey) attributeDefinition.attribute());
			}

			return entity.get(attributeDefinition.attribute());
		}

		return defaultValue(attributeDefinition.attribute()).get().get();
	}

	private static void addColumnValues(ValueSupplier valueSupplier, EntityDefinition definition, Entity newEntity) {
		definition.columns().definitions().stream()
						//these are set via their respective parent foreign key
						.filter(columnDefinition -> !definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()))
						.filter(columnDefinition -> !columnDefinition.columnHasDefaultValue() || columnDefinition.hasDefaultValue())
						.map(columnDefinition -> (AttributeDefinition<Object>) columnDefinition)
						.forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
	}

	private static void addTransientValues(ValueSupplier valueSupplier, EntityDefinition definition, Entity newEntity) {
		definition.attributes().definitions().stream()
						.filter(TransientAttributeDefinition.class::isInstance)
						.filter(attributeDefinition -> !attributeDefinition.derived())
						.map(attributeDefinition -> (AttributeDefinition<Object>) attributeDefinition)
						.forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
	}

	private static void addForeignKeyValues(ValueSupplier valueSupplier, EntityDefinition definition, Entity newEntity) {
		definition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
						newEntity.put(foreignKeyDefinition.attribute(), valueSupplier.get(foreignKeyDefinition)));
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

	private static Collection<Entity> entityForInsert(AbstractEntityEditModel editModel) {
		Entity toInsert = editModel.entity.copy();
		if (toInsert.definition().primaryKey().generated()) {
			toInsert.clearPrimaryKey();
		}

		return singleton(toInsert);
	}

	private interface ValueSupplier {
		<T> T get(AttributeDefinition<T> attributeDefinition);
	}

	private final class DefaultInsert implements Insert {

		private final Collection<Entity> entities;
		private final boolean activeEntity;

		private DefaultInsert() throws ValidationException {
			this(entityForInsert(AbstractEntityEditModel.this), true);
		}

		private DefaultInsert(Collection<Entity> entities) throws ValidationException {
			this(entities, false);
		}

		private DefaultInsert(Collection<Entity> entities, boolean activeEntity) throws ValidationException {
			this.entities = unmodifiableCollection(new ArrayList<>(entities));
			this.activeEntity = activeEntity;
			states.verifyInsertEnabled();
			validate(entities);
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
				Collection<Entity> inserted = unmodifiableCollection(insert(entities, connectionProvider.connection()));
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
				notifyAfterInsert(insertedEntities);
				if (activeEntity) {
					setEntity(insertedEntities.iterator().next());
				}

				return insertedEntities;
			}
		}
	}

	private final class DefaultUpdate implements Update {

		private final Collection<Entity> entities;

		private DefaultUpdate() throws ValidationException {
			this.entities = singleton(entity.copy());
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

				return new UpdateResult(update(entities, connectionProvider.connection()));
			}
		}

		private final class UpdateResult implements Result {

			private final Collection<Entity> updatedEntities;

			private UpdateResult(Collection<Entity> updatedEntities) {
				this.updatedEntities = updatedEntities;
			}

			@Override
			public Collection<Entity> handle() {
				notifyAfterUpdate(originalPrimaryKeyMap(entities, updatedEntities));
				Entity activeEntity = entity();
				updatedEntities.stream()
								.filter(updatedEntity -> updatedEntity.equals(activeEntity))
								.findFirst()
								.ifPresent(AbstractEntityEditModel.this::setEntity);

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
			Entity copy = entity.copy();
			copy.revert();

			return copy;
		}

		private final class DeleteTask implements Task {

			@Override
			public Result perform() throws DatabaseException {
				LOG.debug("{} - delete {}", this, entities);
				delete(entities, connectionProvider.connection());

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
				if (activeEntity) {
					defaults();
				}

				return deletedEntities;
			}
		}
	}

	private final class NotifyInserted implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> insertedEntities) {
			if (states.postEditEvents.get()) {
				EntityEditEvents.inserted(insertedEntities);
			}
		}
	}

	private final class NotifyUpdated implements Consumer<Map<Entity.Key, Entity>> {

		@Override
		public void accept(Map<Entity.Key, Entity> updatedEntities) {
			if (states.postEditEvents.get()) {
				EntityEditEvents.updated(updatedEntities);
			}
		}
	}

	private final class NotifyDeleted implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deletedEntities) {
			if (states.postEditEvents.get()) {
				EntityEditEvents.deleted(deletedEntities);
			}
		}
	}

	private final class Events {

		private final Event<Collection<Entity>> beforeInsert = Event.event();
		private final Event<Collection<Entity>> afterInsert = Event.event();
		private final Event<Map<Entity.Key, Entity>> beforeUpdate = Event.event();
		private final Event<Map<Entity.Key, Entity>> afterUpdate = Event.event();
		private final Event<Collection<Entity>> beforeDelete = Event.event();
		private final Event<Collection<Entity>> afterDelete = Event.event();
		private final Event<?> afterInsertUpdateOrDelete = Event.event();
		private final Event<Entity> entityChanging = Event.event();
		private final Event<Entity> entityChanged = Event.event();
		private final Event<Attribute<?>> valueChange = Event.event();
		private final Map<Attribute<?>, Event<?>> editEvents = new ConcurrentHashMap<>();

		private void bindEvents() {
			afterInsert.addListener(afterInsertUpdateOrDelete);
			afterUpdate.addListener(afterInsertUpdateOrDelete);
			afterDelete.addListener(afterInsertUpdateOrDelete);
			afterInsert.addConsumer(new NotifyInserted());
			afterUpdate.addConsumer(new NotifyUpdated());
			afterDelete.addConsumer(new NotifyDeleted());
			validator.addListener(states::updateValidState);
			modifiedPredicate.addListener(states::updateModifiedState);
			existsPredicate.addListener(states::updateExistsState);
		}

		private <T> EventObserver<T> valueEdited(Attribute<T> attribute) {
			entityDefinition().attributes().definition(attribute);
			return ((Event<T>) editEvents.computeIfAbsent(attribute, k -> Event.event())).observer();
		}

		private <T> void notifyValueEdit(Attribute<T> attribute, T value, Map<Attribute<?>, Object> dependingValues) {
			notifyValueChange(attribute);
			Event<T> editEvent = (Event<T>) editEvents.get(attribute);
			if (editEvent != null) {
				editEvent.accept(value);
			}
			dependingValues.forEach((dependingAttribute, previousValue) -> {
				Object currentValue = AbstractEntityEditModel.this.entity.get(dependingAttribute);
				if (!Objects.equals(previousValue, currentValue)) {
					notifyValueEdit((Attribute<Object>) dependingAttribute, currentValue, emptyMap());
				}
			});
		}

		private void notifyValueChange(Attribute<?> attribute) {
			states.updateEntityStates();
			states.updateAttributeStates(attribute);
			EditModelValue<?> editModelValue = editModelValues.get(attribute);
			if (editModelValue != null) {
				editModelValue.valueChanged();
			}
			valueChange.accept(attribute);
		}
	}

	private final class States {

		private final State entityValid = State.state();
		private final State entityExists = State.state(false);
		private final State entityModified = State.state();
		private final StateObserver editing = State.and(entityExists, entityModified);
		private final State primaryKeyNull = State.state(true);
		private final State readOnly = State.state();
		private final State insertEnabled = State.state(true);
		private final State updateEnabled = State.state(true);
		private final State updateMultipleEnabled = State.state(true);
		private final State deleteEnabled = State.state(true);
		private final State postEditEvents = State.state(POST_EDIT_EVENTS.get());
		private final Map<Attribute<?>, State> attributeModifiedMap = new HashMap<>();
		private final Map<Attribute<?>, State> attributeNullMap = new HashMap<>();
		private final Map<Attribute<?>, State> attributeValidMap = new HashMap<>();

		private StateObserver modifiedObserver(Attribute<?> attribute) {
			entityDefinition().attributes().definition(attribute);
			return attributeModifiedMap.computeIfAbsent(attribute, k ->
							State.state(entityExists.get() && entity.modified(attribute))).observer();
		}

		private StateObserver nullObserver(Attribute<?> attribute) {
			entityDefinition().attributes().definition(attribute);
			return attributeNullMap.computeIfAbsent(attribute, k ->
							State.state(entity.isNull(attribute))).observer();
		}

		private StateObserver validObserver(Attribute<?> attribute) {
			entityDefinition().attributes().definition(attribute);
			return attributeValidMap.computeIfAbsent(attribute, k ->
							State.state(valid(attribute))).observer();
		}

		private void updateEntityStates() {
			updateExistsState();
			updateModifiedState();
			updateValidState();
			updatePrimaryKeyNullState();
		}

		private void updateExistsState() {
			entityExists.set(existsPredicate.get().test(entity));
		}

		private void updateModifiedState() {
			entityModified.set(modifiedPredicate.get().test(entity));
		}

		private void updateValidState() {
			entityValid.set(validator.get().valid(entity));
		}

		private void updatePrimaryKeyNullState() {
			primaryKeyNull.set(entity.primaryKey().isNull());
		}

		private <T> void updateAttributeStates(Attribute<T> attribute) {
			State nullState = attributeNullMap.get(attribute);
			if (nullState != null) {
				nullState.set(entity.isNull(attribute));
			}
			State validState = attributeValidMap.get(attribute);
			if (validState != null) {
				validState.set(valid(attribute));
			}
			State modifiedState = attributeModifiedMap.get(attribute);
			if (modifiedState != null) {
				updateAttributeModifiedState(attribute, modifiedState);
			}
		}

		private boolean valid(Attribute<?> attribute) {
			try {
				validate(attribute);
				return true;
			}
			catch (ValidationException e) {
				return false;
			}
		}

		private void updateAttributeModifiedStates() {
			attributeModifiedMap.forEach(this::updateAttributeModifiedState);
		}

		private void updateAttributeModifiedState(Attribute<?> attribute, State modifiedState) {
			modifiedState.set(existsPredicate.get().test(entity) && entity.modified(attribute));
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
				throw new IllegalStateException("Batch update of entities is not enabled");
			}
		}

		private void verifyDeleteEnabled() {
			if (readOnly.get() || !deleteEnabled.get()) {
				throw new IllegalStateException("Edit model is readOnly or deleting is not enabled!");
			}
		}
	}

	private final class EditModelValue<T> extends AbstractValue<T> {

		private final Attribute<T> attribute;

		private EditModelValue(Attribute<T> attribute) {
			this.attribute = attribute;
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
				events.notifyValueEdit(attribute, value, dependingValues);
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
			entityDefinition().attributes().derivedFrom(attribute).forEach(derivedAttribute -> {
				dependingValues.put(derivedAttribute, entity.get(derivedAttribute));
				addDependingDerivedAttributes(derivedAttribute, dependingValues);
			});
		}

		private void addDependingForeignKeys(Column<?> column, Map<Attribute<?>, Object> dependingValues) {
			entityDefinition().foreignKeys().definitions(column).forEach(foreignKeyDefinition ->
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