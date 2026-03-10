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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.ObservableValueSet;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueChange;
import is.codion.common.reactive.value.ValueSet;
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
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.AttributeValidationException;
import is.codion.framework.domain.entity.exception.EntityValidationException;

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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A default {@link EntityEditor} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public class DefaultEntityEditor<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
				T extends EntityTableModel<M, E, T, R>, R extends EntityEditor<M, E, T, R>> implements EntityEditor<M, E, T, R> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditor.class);

	private static final ValueSupplier INITIAL_VALUE = new InitialValue();

	private final Map<Attribute<?>, Event<?>> editEvents = new HashMap<>();
	private final Event<Attribute<?>> valueChanged = Event.event();

	private final Map<Attribute<?>, DefaultEditorValue<?>> editorValues = new HashMap<>();
	private final Map<Attribute<?>, State> persistValues = new HashMap<>();
	private final Map<Attribute<?>, State> attributeModified = new HashMap<>();
	private final Map<Attribute<?>, State> attributePresent = new HashMap<>();
	private final Map<Attribute<?>, State> attributeValid = new HashMap<>();
	private final Map<Attribute<?>, Value<String>> messages = new HashMap<>();

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final State primaryKeyPresent = State.state(false);
	private final State valid = State.state();
	private final DefaultExists exists;
	private final DefaultModified modified;
	private final Value<EntityValidator> validator;
	private final ComponentModels<M, E, T, R> componentModels;
	private final SearchModels searchModels = new DefaultSearchModels();
	private final EditorPersistence persistence;

	private final DefaultEditorEntity entity;

	/**
	 * Instantiates a new {@link DefaultEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 * @param componentModels the editor component models
	 */
	public DefaultEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider,
														 ComponentModels<M, E, T, R> componentModels) {
		this.entityDefinition = requireNonNull(connectionProvider).entities().definition(entityType);
		this.connectionProvider = requireNonNull(connectionProvider);
		this.componentModels = requireNonNull(componentModels);
		this.persistence = new DefaultEditorPersistence();
		this.entity = new DefaultEditorEntity(createEntity(INITIAL_VALUE));
		this.exists = new DefaultExists(entityDefinition);
		this.modified = new DefaultModified();
		this.validator = Value.builder()
						.nonNull(entityDefinition.validator())
						.listener(this::updateValidStates)
						.build();
		configurePersistentForeignKeys();
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
	public final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public final EditorEntity entity() {
		return entity;
	}

	@Override
	public final void clear() {
		entity.set(entityDefinition.entity());
	}

	@Override
	public final void defaults() {
		entity.set(null);
	}

	@Override
	public final void revert() {
		entityDefinition.attributes().get().forEach(attribute -> value(attribute).revert());
	}

	@Override
	public final Exists exists() {
		return exists;
	}

	@Override
	public final Modified modified() {
		return modified;
	}

	@Override
	public final Observer<Attribute<?>> valueChanged() {
		return valueChanged.observer();
	}

	@Override
	public final ObservableState primaryKeyPresent() {
		return primaryKeyPresent.observable();
	}

	@Override
	public final Value<EntityValidator> validator() {
		return validator;
	}

	@Override
	public final ObservableState valid() {
		return valid.observable();
	}

	@Override
	public final void validate() throws EntityValidationException {
		validate(entity.get());
	}

	@Override
	public final void validate(Attribute<?> attribute) throws AttributeValidationException {
		validator.getOrThrow().validate(entity.get(), attribute);
	}

	@Override
	public final void validate(Collection<Entity> entities) throws EntityValidationException {
		for (Entity entityToValidate : requireNonNull(entities)) {
			validate(entityToValidate);
		}
	}

	@Override
	public final void validate(Entity entity) throws EntityValidationException {
		if (entity.type().equals(entityDefinition.type())) {
			validator.getOrThrow().validate(entity);
		}
		else {
			entity.definition().validator().validate(entity);
		}
	}

	@Override
	public final <T> EditorValue<T> value(Attribute<T> attribute) {
		return (EditorValue<T>) editorValues.computeIfAbsent(attribute, DefaultEditorValue::new);
	}

	@Override
	public final SearchModels searchModels() {
		return searchModels;
	}

	@Override
	public final EditorPersistence persistence() {
		return persistence;
	}

	@Override
	public final PersistTasks tasks(EntityConnection connection) {
		return new DefaultPersistTasks(requireNonNull(connection));
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entityDefinition.type();
	}

	/**
	 * @return the {@link ComponentModels} instance
	 */
	protected ComponentModels<M, E, T, R> componentModels() {
		return componentModels;
	}

	private void notifyValueChange(Attribute<?> attribute, Map<Attribute<?>, String> invalidAttributes) {
		updateAttributeStates(attribute, invalidAttributes);
		DefaultEditorValue<?> editorValue = editorValues.get(attribute);
		if (editorValue != null) {
			editorValue.valueChanged();
		}
		valueChanged.accept(attribute);
	}

	private Map<Attribute<?>, String> updateStates() {
		Entity instance = entity.get();
		exists.update(instance);
		modified.update();
		primaryKeyPresent.set(!entity.instance.primaryKey().isNull());

		return updateEntityValidState(instance);
	}

	private void updateAttributeStates(Attribute<?> attribute, Map<Attribute<?>, String> invalid) {
		State presentState = attributePresent.get(attribute);
		if (presentState != null) {
			presentState.set(!entity.instance.isNull(attribute));
		}
		State validState = attributeValid.get(attribute);
		if (validState != null) {
			validState.set(!invalid.containsKey(attribute));
		}
		State modifiedState = attributeModified.get(attribute);
		if (modifiedState != null) {
			updateAttributeModifiedState(attribute, modifiedState);
		}
		Value<String> message = messages.get(attribute);
		if (message != null) {
			message.set(createMessage(attribute, invalid.get(attribute)));
		}
	}

	private @Nullable String createMessage(Attribute<?> attribute, @Nullable String validationMessage) {
		String description = entityDefinition.attributes().definition(attribute).description().orElse(null);
		if (nullOrEmpty(validationMessage)) {
			return description;
		}
		else if (nullOrEmpty(description)) {
			return validationMessage;
		}

		return Stream.of(validationMessage, description)
						.collect(joining("<br>", "<html>", "</html"));
	}

	private void updateAttributeModifiedState(Attribute<?> attribute, State modifiedState) {
		modifiedState.set(exists.is() && entity.instance.modified(attribute));
	}

	private Map<Attribute<?>, String> updateEntityValidState() {
		return updateEntityValidState(entity.get());
	}

	private Map<Attribute<?>, String> updateEntityValidState(Entity instance) {
		try {
			validate(instance);
			valid.set(true);

			return emptyMap();
		}
		catch (EntityValidationException e) {
			valid.set(false);

			return e.attributes().stream()
							.collect(toMap(AttributeValidationException::attribute, Throwable::getMessage));
		}
	}

	private void updateValidStates() {
		Map<Attribute<?>, String> invalid = updateEntityValidState();
		attributeValid.forEach((attribute, state) -> state.set(!invalid.containsKey(attribute)));
		messages.forEach((attribute, value) -> value.set(invalid.get(attribute)));
	}

	/**
	 * Instantiates a new {@link Entity} using the values provided by {@code valueSupplier}.
	 * Values are populated for {@link ValueAttributeDefinition} and its descendants.
	 * If a {@link ColumnDefinition}s underlying column has a default value the attribute is
	 * skipped unless the attribute itself has a default value, which then overrides the columns default value.
	 * @return an entity instance populated with default values
	 * @see ValueAttributeDefinition.Builder#withDefault(boolean)
	 * @see ValueAttributeDefinition.Builder#defaultValue(Object)
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
						.filter(columnDefinition -> !columnDefinition.withDefault() || columnDefinition.hasDefaultValue())
						.filter(ColumnDefinition::selected) // don't populate lazy loaded values
						.map(columnDefinition -> (ColumnDefinition<Object>) columnDefinition)
						.forEach(columnDefinition -> newEntity.set(columnDefinition.attribute(), valueSupplier.get(columnDefinition)));
	}

	private void addTransientValues(ValueSupplier valueSupplier, Entity newEntity) {
		entityDefinition.attributes().definitions().stream()
						.filter(TransientAttributeDefinition.class::isInstance)
						.map(TransientAttributeDefinition.class::cast)
						.map(attributeDefinition -> (TransientAttributeDefinition<Object>) attributeDefinition)
						.forEach(attributeDefinition -> newEntity.set(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
	}

	private void addForeignKeyValues(ValueSupplier valueSupplier, Entity newEntity) {
		entityDefinition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
						newEntity.set(foreignKeyDefinition.attribute(), valueSupplier.get(foreignKeyDefinition)));
	}

	private void configurePersistentForeignKeys() {
		if (PERSIST_FOREIGN_KEYS.getOrThrow()) {
			entityDefinition.foreignKeys().get().forEach(foreignKey ->
							value(foreignKey).persist().set(foreignKeyWritable(foreignKey)));
		}
	}

	private boolean foreignKeyWritable(ForeignKey foreignKey) {
		return foreignKey.references().stream()
						.map(ForeignKey.Reference::column)
						.map(entityDefinition.columns()::definition)
						.map(ColumnDefinition.class::cast)
						.anyMatch(columnDefinition -> !columnDefinition.readOnly());
	}

	private void addEditListeners() {
		entityDefinition.foreignKeys().get().stream()
						.map(ForeignKey::referencedType)
						.distinct()
						.forEach(entityType -> {
							PersistenceEvents persistenceEvents = persistenceEvents(entityType);
							persistenceEvents.updated().addWeakConsumer(updateListener);
							persistenceEvents.deleted().addWeakConsumer(deleteListener);
						});
	}

	private interface ValueSupplier {
		<T> @Nullable T get(AttributeDefinition<T> attributeDefinition);
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			groupByType(deleted).forEach((key, value) ->
							entityDefinition.foreignKeys().get(key)
											.forEach(foreignKey -> deleted(foreignKey, value)));
		}

		private void deleted(ForeignKey foreignKey, Collection<Entity> entities) {
			Entity currentForeignKeyValue = value(foreignKey).get();
			if (currentForeignKeyValue != null && entities.contains(currentForeignKeyValue)) {
				value(foreignKey).clear();
				LOG.debug("{} - cleared FK {} (referenced entity deleted)", this, foreignKey);
			}
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Map<EntityType, Map<Entity.Key, Entity>> grouped = new HashMap<>();
			updated.forEach((beforeUpdate, afterUpdate) ->
							grouped.computeIfAbsent(beforeUpdate.type(), k -> new HashMap<>())
											.put(beforeUpdate.originalPrimaryKey(), afterUpdate));
			grouped.forEach((entityType, entities) ->
							entityDefinition.foreignKeys().get(entityType)
											.forEach(foreignKey -> updated(foreignKey, entities)));
		}

		private void updated(ForeignKey foreignKey, Map<Entity.Key, Entity> entities) {
			Entity currentForeignKeyValue = value(foreignKey).get();
			if (currentForeignKeyValue != null && entities.containsKey(currentForeignKeyValue.primaryKey())) {
				value(foreignKey).clear();
				value(foreignKey).set(entities.get(currentForeignKeyValue.primaryKey()));
				LOG.debug("{} - updated FK {}", this, foreignKey);
			}
		}
	}

	private final class DefaultEditorEntity implements EditorEntity {

		private final Event<Entity> changing = Event.event();
		private final Event<Entity> changed = Event.event();

		private final Entity instance;

		private DefaultEditorEntity(Entity instance) {
			this.instance = instance;
		}

		@Override
		public Entity get() {
			return instance.immutable();
		}

		@Override
		public Observer<Entity> observer() {
			return changed.observer();
		}

		@Override
		public void set(@Nullable Entity entity) {
			entity = entity == null ? null : validateType(entity).immutable();
			changing.accept(entity);
			setOrDefaults(entity);
		}

		@Override
		public Observer<Entity> changing() {
			return changing.observer();
		}

		@Override
		public void replace(Entity entity) {
			setOrDefaults(validateType(entity));
		}

		@Override
		public void refresh() {
			if (exists.is()) {
				set(connectionProvider.connection().selectSingle(where(key(instance.originalPrimaryKey()))
								.include(entityDefinition.columns().definitions().stream()
												.filter(definition -> !definition.selected())
												.map(ColumnDefinition::attribute)
												.filter(instance::contains)
												.collect(toSet()))
								.build()));
			}
		}

		private void setOrDefaults(@Nullable Entity entity) {
			Map<Attribute<?>, Object> affectedAttributes = instance.set(entity == null ? createEntity(this::defaultValue) : entity);
			Map<Attribute<?>, String> invalidAttributes = updateStates();
			for (Attribute<?> affectedAttribute : affectedAttributes.keySet()) {
				notifyValueChange(affectedAttribute, invalidAttributes);
			}
			attributeModified.forEach(DefaultEntityEditor.this::updateAttributeModifiedState);

			changed.accept(entity);
		}

		private <T> @Nullable T defaultValue(AttributeDefinition<T> attributeDefinition) {
			if (value(attributeDefinition.attribute()).persist().is()) {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					return (T) instance.entity((ForeignKey) attributeDefinition.attribute());
				}

				return instance.get(attributeDefinition.attribute());
			}

			return value(attributeDefinition.attribute()).defaultValue().getOrThrow().get();
		}

		private Entity validateType(Entity entity) {
			requireNonNull(entity);
			if (!entityDefinition.type().equals(entity.type())) {
				throw new IllegalStateException("Entity type mismatch for entity: " + entity);
			}

			return entity;
		}
	}

	private final class DefaultPersistTasks implements PersistTasks {

		private static final String NOT_MODIFIED = "Entity is not modified: ";

		private final EntityConnection connection;

		private DefaultPersistTasks(EntityConnection connection) {
			this.connection = connection;
		}

		@Override
		public InsertEntityTaskBuilder insert() throws EntityValidationException {
			validate();

			return new DefaultInsertEntityTaskBuilder(entity().get().copy().mutable())
							.after(insertedEntity -> entity().replace(insertedEntity));
		}

		@Override
		public InsertEntityTaskBuilder insert(Entity entity) throws EntityValidationException {
			validate(requireNonNull(entity));

			return new DefaultInsertEntityTaskBuilder(entity.copy().mutable());
		}

		@Override
		public InsertEntitiesTaskBuilder insert(Collection<Entity> entities) throws EntityValidationException {
			validate(requireNonNull(entities));

			return new DefaultInsertEntitiesTaskBuilder(entities);
		}

		@Override
		public UpdateEntityTaskBuilder update() throws EntityValidationException {
			validate();
			if (!modified().is()) {
				throw new IllegalStateException(NOT_MODIFIED + entity().get());
			}

			return new DefaultUpdateEntityTaskBuilder(entity().get().copy().mutable())
							.after((beforeUpdate, afterUpdate) -> entity().replace(afterUpdate));
		}

		@Override
		public UpdateEntityTaskBuilder update(Entity entity) throws EntityValidationException {
			validate(requireNonNull(entity));
			if (!entity.modified()) {
				throw new IllegalStateException(NOT_MODIFIED + entity);
			}

			return new DefaultUpdateEntityTaskBuilder(entity.copy().mutable());
		}

		@Override
		public UpdateEntitiesTaskBuilder update(Collection<Entity> entities) throws EntityValidationException {
			validate(requireNonNull(entities));
			for (Entity entity : entities) {
				if (!entity.modified()) {
					throw new IllegalStateException(NOT_MODIFIED + entity);
				}
			}

			return new DefaultUpdateEntitiesTaskBuilder(entities);
		}

		@Override
		public DeleteEntityTaskBuilder delete() {
			return new DefaultDeleteEntityTaskBuilder(entity().get().copy().mutable())
							.after(deletedEntities -> defaults());
		}

		@Override
		public DeleteEntityTaskBuilder delete(Entity entity) {
			return new DefaultDeleteEntityTaskBuilder(requireNonNull(entity).copy().mutable());
		}

		@Override
		public DeleteEntitiesTaskBuilder delete(Collection<Entity> entities) {
			return new DefaultDeleteEntitiesTaskBuilder(requireNonNull(entities));
		}

		private final class DefaultInsertEntityTaskBuilder implements InsertEntityTaskBuilder {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before = new ArrayList<>();
			private final Collection<Consumer<Entity>> after = new ArrayList<>();

			private DefaultInsertEntityTaskBuilder(Entity entity) {
				this.entity = entity;
			}

			@Override
			public InsertEntityTaskBuilder before(Consumer<Entity> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public InsertEntityTaskBuilder after(Consumer<Entity> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Entity> build() {
				return new DefaultInsertEntity(entity, before, after);
			}
		}

		private final class DefaultInsertEntitiesTaskBuilder implements InsertEntitiesTaskBuilder {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before = new ArrayList<>();
			private final Collection<Consumer<Collection<Entity>>> after = new ArrayList<>();

			private DefaultInsertEntitiesTaskBuilder(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public InsertEntitiesTaskBuilder before(Consumer<Collection<Entity>> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public InsertEntitiesTaskBuilder after(Consumer<Collection<Entity>> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Collection<Entity>> build() {
				return new DefaultInsertEntitiesTask(entities, before, after);
			}
		}

		private final class DefaultUpdateEntityTaskBuilder implements UpdateEntityTaskBuilder {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before = new ArrayList<>();
			private final Collection<BiConsumer<Entity, Entity>> after = new ArrayList<>();

			private DefaultUpdateEntityTaskBuilder(Entity entity) {
				this.entity = entity;
			}

			@Override
			public UpdateEntityTaskBuilder before(Consumer<Entity> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public UpdateEntityTaskBuilder after(BiConsumer<Entity, Entity> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Entity> build() {
				return new UpdateEntity(entity, before, after);
			}
		}

		private final class DefaultUpdateEntitiesTaskBuilder implements UpdateEntitiesTaskBuilder {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before = new ArrayList<>();
			private final Collection<Consumer<Map<Entity, Entity>>> after = new ArrayList<>();

			private DefaultUpdateEntitiesTaskBuilder(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public UpdateEntitiesTaskBuilder before(Consumer<Collection<Entity>> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public UpdateEntitiesTaskBuilder after(Consumer<Map<Entity, Entity>> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Collection<Entity>> build() {
				return new UpdateEntities(entities, before, after);
			}
		}

		private final class DefaultDeleteEntityTaskBuilder implements DeleteEntityTaskBuilder {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before = new ArrayList<>();
			private final Collection<Consumer<Entity>> after = new ArrayList<>();

			private DefaultDeleteEntityTaskBuilder(Entity entity) {
				this.entity = entity;
			}

			@Override
			public DeleteEntityTaskBuilder before(Consumer<Entity> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public DeleteEntityTaskBuilder after(Consumer<Entity> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Entity> build() {
				return new DeleteEntity(entity, before, after);
			}
		}

		private final class DefaultDeleteEntitiesTaskBuilder implements DeleteEntitiesTaskBuilder {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before = new ArrayList<>();
			private final Collection<Consumer<Collection<Entity>>> after = new ArrayList<>();

			private DefaultDeleteEntitiesTaskBuilder(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public DeleteEntitiesTaskBuilder before(Consumer<Collection<Entity>> before) {
				this.before.add(requireNonNull(before));
				return this;
			}

			@Override
			public DeleteEntitiesTaskBuilder after(Consumer<Collection<Entity>> after) {
				this.after.add(requireNonNull(after));
				return this;
			}

			@Override
			public PersistTask<Collection<Entity>> build() {
				return new DeleteEntities(entities, before, after);
			}
		}

		private final class DefaultInsertEntity implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before;
			private final Collection<Consumer<Entity>> after;

			private DefaultInsertEntity(Entity entity,
																	Collection<Consumer<Entity>> before,
																	Collection<Consumer<Entity>> after) {
				this.entity = entity;
				this.before = before;
				this.after = after;
			}

			@Override
			public Task<Entity> prepare() {
				before.forEach(consumer -> consumer.accept(entity));

				return new DefaultInsertEntity.InsertTask();
			}

			private final class InsertTask implements Task<Entity> {

				@Override
				public Result<Entity> perform() {
					LOG.debug("insert {}", entity);

					return new InsertResult(persistence.get().insert(entity, connection));
				}
			}

			private final class InsertResult implements Result<Entity> {

				private final Entity insertedEntity;

				private InsertResult(Entity insertedEntity) {
					this.insertedEntity = insertedEntity;
				}

				@Override
				public Entity handle() {
					after.forEach(consumer -> consumer.accept(insertedEntity));

					return insertedEntity;
				}
			}
		}

		private final class DefaultInsertEntitiesTask implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before;
			private final Collection<Consumer<Collection<Entity>>> after;

			private DefaultInsertEntitiesTask(Collection<Entity> entities,
																				Collection<Consumer<Collection<Entity>>> before,
																				Collection<Consumer<Collection<Entity>>> after) {
				this.entities = unmodifiableCollection(new ArrayList<>(entities));
				this.before = before;
				this.after = after;
			}

			@Override
			public Task<Collection<Entity>> prepare() {
				before.forEach(consumer -> consumer.accept(entities));

				return new DefaultInsertEntitiesTask.InsertTask();
			}

			private final class InsertTask implements Task<Collection<Entity>> {

				@Override
				public Result<Collection<Entity>> perform() {
					LOG.debug("insert {}", entities);

					return new InsertResult(unmodifiableCollection(persistence.get().insert(entities, connection)));
				}
			}

			private final class InsertResult implements Result<Collection<Entity>> {

				private final Collection<Entity> insertedEntities;

				private InsertResult(Collection<Entity> insertedEntities) {
					this.insertedEntities = insertedEntities;
				}

				@Override
				public Collection<Entity> handle() {
					after.forEach(consumer -> consumer.accept(insertedEntities));

					return insertedEntities;
				}
			}
		}

		private final class UpdateEntity implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before;
			private final Collection<BiConsumer<Entity, Entity>> after;

			private UpdateEntity(Entity entity,
													 Collection<Consumer<Entity>> before,
													 Collection<BiConsumer<Entity, Entity>> after) {
				this.entity = entity;
				this.before = before;
				this.after = after;
			}

			@Override
			public Task<Entity> prepare() {
				before.forEach(consumer -> consumer.accept(entity));

				return new UpdateEntity.UpdateTask();
			}

			private final class UpdateTask implements Task<Entity> {

				@Override
				public Result<Entity> perform() {
					LOG.debug("update {}", entity);

					return new UpdateResult(persistence.get().update(entity, connection));
				}
			}

			private final class UpdateResult implements Result<Entity> {

				private final Entity updatedEntity;

				private UpdateResult(Entity updatedEntity) {
					this.updatedEntity = updatedEntity;
				}

				@Override
				public Entity handle() {
					after.forEach(consumer -> consumer.accept(entity.immutable(), updatedEntity));

					return updatedEntity;
				}
			}
		}

		private final class UpdateEntities implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before;
			private final Collection<Consumer<Map<Entity, Entity>>> after;

			private UpdateEntities(Collection<Entity> entities,
														 Collection<Consumer<Collection<Entity>>> before,
														 Collection<Consumer<Map<Entity, Entity>>> after) {
				this.entities = unmodifiableCollection(new ArrayList<>(entities));
				this.before = before;
				this.after = after;
			}

			@Override
			public Task<Collection<Entity>> prepare() {
				before.forEach(consumer -> consumer.accept(entities));

				return new UpdateEntities.UpdateTask();
			}

			private final class UpdateTask implements Task<Collection<Entity>> {

				@Override
				public Result<Collection<Entity>> perform() {
					LOG.debug("update {}", entities);

					return new UpdateResult(persistence.get().update(entities, connection));
				}
			}

			private final class UpdateResult implements Result<Collection<Entity>> {

				private final Collection<Entity> updatedEntities;

				private UpdateResult(Collection<Entity> updatedEntities) {
					this.updatedEntities = updatedEntities;
				}

				@Override
				public Collection<Entity> handle() {
					after.forEach(consumer -> consumer.accept(originalEntityMap(entities, updatedEntities)));

					return updatedEntities;
				}
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
		}

		private final class DeleteEntity implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<Consumer<Entity>> before;
			private final Collection<Consumer<Entity>> after;

			private DeleteEntity(Entity entity,
													 Collection<Consumer<Entity>> before,
													 Collection<Consumer<Entity>> after) {
				this.entity = entity;
				this.before = before;
				this.after = after;
				entity.revert();
			}

			@Override
			public Task<Entity> prepare() {
				before.forEach(consumer -> consumer.accept(entity));

				return new DeleteEntity.DeleteTask();
			}

			private final class DeleteTask implements Task<Entity> {

				@Override
				public Result<Entity> perform() {
					LOG.debug("delete {}", entity);
					persistence.get().delete(entity, connection);

					return new DeleteEntity.DeleteResult(entity);
				}
			}

			private final class DeleteResult implements Result<Entity> {

				private final Entity deletedEntity;

				private DeleteResult(Entity deletedEntity) {
					this.deletedEntity = deletedEntity;
				}

				@Override
				public Entity handle() {
					after.forEach(consumer -> consumer.accept(deletedEntity));

					return deletedEntity;
				}
			}
		}

		private final class DeleteEntities implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;
			private final Collection<Consumer<Collection<Entity>>> before;
			private final Collection<Consumer<Collection<Entity>>> after;

			private DeleteEntities(Collection<Entity> entities,
														 Collection<Consumer<Collection<Entity>>> before,
														 Collection<Consumer<Collection<Entity>>> after) {
				this.entities = unmodifiableCollection(new ArrayList<>(entities));
				this.before = before;
				this.after = after;
			}

			@Override
			public Task<Collection<Entity>> prepare() {
				before.forEach(consumer -> consumer.accept(entities));

				return new DeleteEntities.DeleteTask();
			}

			private final class DeleteTask implements Task<Collection<Entity>> {

				@Override
				public Result<Collection<Entity>> perform() {
					LOG.debug("delete {}", entities);
					persistence.get().delete(entities, connection);

					return new DeleteEntities.DeleteResult(entities);
				}
			}

			private final class DeleteResult implements Result<Collection<Entity>> {

				private final Collection<Entity> deletedEntities;

				private DeleteResult(Collection<Entity> deletedEntities) {
					this.deletedEntities = deletedEntities;
				}

				@Override
				public Collection<Entity> handle() {
					after.forEach(consumer -> consumer.accept(deletedEntities));

					return deletedEntities;
				}
			}
		}
	}

	private static class DefaultEditorPersistence implements EditorPersistence {

		private static final EntityPersistence DEFAULT = new EntityPersistence() {};

		private EntityPersistence instance = DEFAULT;

		@Override
		public EntityPersistence get() {
			return instance;
		}

		@Override
		public void set(@Nullable EntityPersistence persistence) {
			if (!instance.replaceable()) {
				throw new IllegalStateException("Current EntityPersistence implementation is not replaceable");
			}
			this.instance = persistence == null ? DEFAULT : persistence;
		}
	}

	private final class DefaultSearchModels implements SearchModels {

		private final Map<ForeignKey, EntitySearchModel> searchModels = new HashMap<>();

		@Override
		public EntitySearchModel get(ForeignKey foreignKey) {
			entityDefinition().foreignKeys().definition(foreignKey);
			synchronized (searchModels) {
				// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
				// box models, create() may for example call this function
				// see javadoc: must not attempt to update any other mappings of this map
				EntitySearchModel entitySearchModel = searchModels.get(foreignKey);
				if (entitySearchModel == null) {
					entitySearchModel = create(foreignKey);
					searchModels.put(foreignKey, entitySearchModel);
				}

				return entitySearchModel;
			}
		}

		@Override
		public EntitySearchModel create(ForeignKey foreignKey) {
			return componentModels.searchModel(foreignKey, connectionProvider);
		}
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
		public ObservableState not() {
			return exists.not();
		}

		@Override
		public boolean is() {
			return exists.is();
		}

		@Override
		public Observer<Boolean> observer() {
			return exists.observer();
		}

		private void update() {
			update(entity.get());
		}

		private void update(Entity instance) {
			exists.set(predicate.getOrThrow().test(instance));
		}
	}

	private final class DefaultModified implements Modified {

		private final State modified = State.state();
		private final ValueSet<Attribute<?>> attributes = ValueSet.valueSet();
		private final Runnable additionalListener = this::update;
		private final ValueSet<ObservableState> additional = ValueSet.<ObservableState>builder()
						.changeConsumer(this::additionalChanged)
						.build();

		@Override
		public ObservableValueSet<Attribute<?>> attributes() {
			return attributes.observable();
		}

		@Override
		public ValueSet<ObservableState> additional() {
			return additional;
		}

		@Override
		public ObservableState not() {
			return modified.not();
		}

		@Override
		public boolean is() {
			return modified.is();
		}

		@Override
		public Observer<Boolean> observer() {
			return modified.observer();
		}

		private void update() {
			boolean existing = exists.is();
			attributes.set(existing ? editorValues.keySet().stream()
							.filter(entity.instance::modified)
							.collect(toSet()) : emptySet());
			modified.set(existing && entity.instance.modified() || additional.get().stream()
							.anyMatch(ObservableState::is));
		}

		private void additionalChanged(ValueChange<Set<ObservableState>> change) {
			change.previous().forEach(state -> state.removeListener(additionalListener));
			change.current().forEach(state -> state.addListener(additionalListener));
			update();
		}
	}

	private final class DefaultEditorValue<T> extends AbstractValue<T> implements EditorValue<T> {

		private static final Propagator<Object> NULL_PROPAGATOR = (value, setter) -> {};

		private final Attribute<T> attribute;
		private final Value<Supplier<T>> defaultValue;
		private final EditorValueSetter editorValueSetter = new EditorValueSetter();

		private Propagator<T> propagator = (Propagator<T>) NULL_PROPAGATOR;

		private DefaultEditorValue(Attribute<T> attribute) {
			super(nullValue(entityDefinition.attributes().definition(attribute)));
			this.attribute = attribute;
			this.defaultValue = Value.nonNull(new DefaultValue(entityDefinition.attributes().definition(attribute)));
		}

		@Override
		public Attribute<T> attribute() {
			return attribute;
		}

		@Override
		public @Nullable T original() {
			return entity.instance.original(attribute);
		}

		@Override
		public void revert() {
			if (modified().is()) {
				super.set(original());
			}
		}

		@Override
		public void validate() {
			State validState = attributeValid.get(attribute);
			if (validState != null) {
				validState.set(isValid(attribute));
			}
		}

		@Override
		public State persist() {
			return persistValues.computeIfAbsent(attribute, k -> State.state());
		}

		@Override
		public ObservableState valid() {
			return attributeValid.computeIfAbsent(attribute,
							k -> State.state(isValid(attribute))).observable();
		}

		@Override
		public ObservableState present() {
			return attributePresent.computeIfAbsent(attribute,
							k -> State.state(!entity.instance.isNull(attribute))).observable();
		}

		@Override
		public Observable<String> message() {
			return messages.computeIfAbsent(attribute,
							k -> createMessage()).observable();
		}

		@Override
		public ObservableState modified() {
			return attributeModified.computeIfAbsent(attribute,
							k -> State.state(exists.is() && entity.instance.modified(attribute))).observable();
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
		public void propagate(@Nullable Propagator<T> propagator) {
			this.propagator = propagator == null ? (Propagator<T>) NULL_PROPAGATOR : propagator;
		}

		@Override
		public void set(Entity entity, @Nullable T value) {
			requireNonNull(entity).set(attribute, value);
			propagator.propagate(value, entity::set);
		}

		@Override
		protected @Nullable T getValue() {
			return entity.instance.get(attribute);
		}

		@Override
		protected void setValue(@Nullable T value) {
			Map<Attribute<?>, Object> dependingValues = dependingValues(attribute);
			T previousValue = entity.instance.set(attribute, value);
			if (!Objects.deepEquals(value, previousValue)) {
				notifyValueEdit(attribute, value, dependingValues);
			}
			propagator.propagate(value, editorValueSetter);
		}

		private <T> void notifyValueEdit(Attribute<T> attribute, @Nullable T value, Map<Attribute<?>, Object> dependingValues) {
			notifyValueEdit(attribute, value, dependingValues, updateStates());
		}

		private <T> void notifyValueEdit(Attribute<T> attribute, @Nullable T value, Map<Attribute<?>, Object> dependingValues,
																		 Map<Attribute<?>, String> invalidAttributes) {
			notifyValueChange(attribute, invalidAttributes);
			Event<T> editEvent = (Event<T>) editEvents.get(attribute);
			if (editEvent != null) {
				editEvent.accept(value);
			}
			dependingValues.forEach((dependingAttribute, previousValue) -> {
				Object currentValue = entity.instance.get(dependingAttribute);
				if (!Objects.deepEquals(previousValue, currentValue)) {
					notifyValueEdit((Attribute<Object>) dependingAttribute, currentValue, emptyMap(), invalidAttributes);
				}
			});
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

		private void addDependingDerivedAttributes(Attribute<?> attribute, Map<Attribute<?>, @Nullable Object> dependingValues) {
			entityDefinition.attributes().derivedFrom(attribute).forEach(derivedAttribute -> {
				dependingValues.put(derivedAttribute, entity.instance.get(derivedAttribute));
				addDependingDerivedAttributes(derivedAttribute, dependingValues);
			});
		}

		private void addDependingForeignKeys(Column<?> column, Map<Attribute<?>, @Nullable Object> dependingValues) {
			entityDefinition.foreignKeys().definitions(column).forEach(foreignKeyDefinition ->
							dependingValues.put(foreignKeyDefinition.attribute(), entity.instance.get(foreignKeyDefinition.attribute())));
		}

		private void addDependingReferencedColumns(ForeignKey foreignKey, Map<Attribute<?>, @Nullable Object> dependingValues) {
			foreignKey.references().forEach(reference ->
							dependingValues.put(reference.column(), entity.instance.get(reference.column())));
		}

		private boolean isValid(Attribute<?> attribute) {
			try {
				validator.getOrThrow().validate(entity.get(), attribute);
				return true;
			}
			catch (AttributeValidationException e) {
				return false;
			}
		}

		private Value<String> createMessage() {
			return Value.nullable(DefaultEntityEditor.this.createMessage(attribute, validationString()));
		}

		private @Nullable String validationString() {
			try {
				DefaultEntityEditor.this.validate(attribute);

				return null;
			}
			catch (AttributeValidationException e) {
				return e.getMessage();
			}
		}

		private final class EditorValueSetter implements Propagator.Setter {

			@Override
			public <T> void set(Attribute<T> attribute, @Nullable T value) {
				value(attribute).set(value);
			}
		}

		private final class DefaultValue implements Supplier<T> {

			private final AttributeDefinition<T> definition;

			private DefaultValue(AttributeDefinition<T> definition) {
				this.definition = definition;
			}

			@Override
			public @Nullable T get() {
				if (definition instanceof ValueAttributeDefinition) {
					return ((ValueAttributeDefinition<T>) definition).defaultValue();
				}

				return null;
			}
		}

		private void valueChanged() {
			notifyObserver();
		}
	}

	private static final class InitialValue implements ValueSupplier {

		@Override
		public <T> @Nullable T get(AttributeDefinition<T> attributeDefinition) {
			if (attributeDefinition instanceof ValueAttributeDefinition<T>) {
				return ((ValueAttributeDefinition<T>) attributeDefinition).defaultValue();
			}

			return null;
		}
	}

	private static <T> @Nullable T nullValue(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition instanceof ValueAttributeDefinition<?>) {
			ValueAttributeDefinition<T> valueAttributeDefinition = (ValueAttributeDefinition<T>) attributeDefinition;
			if (valueAttributeDefinition.attribute().type().isBoolean() && !valueAttributeDefinition.nullable()) {
				return (T) Boolean.FALSE;
			}
		}

		return null;
	}
}
