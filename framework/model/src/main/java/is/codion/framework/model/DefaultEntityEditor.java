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
import is.codion.common.reactive.observer.Change;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.ObservableValueSet;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Validator;
import is.codion.common.reactive.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
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
import is.codion.framework.domain.entity.exception.NullValueException;
import is.codion.framework.model.EditorLink.BeforeInsert;
import is.codion.framework.model.EditorLink.DetailCondition;
import is.codion.framework.model.EditorLink.DetailEntity;
import is.codion.framework.model.EditorLink.DetailSelect;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.db.EntityConnection.transaction;
import static is.codion.framework.domain.entity.Entity.groupByType;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A default {@link EntityEditor} implementation.
 * @param <R> the editor type
 */
public class DefaultEntityEditor<R extends EntityEditor<R>> implements EntityEditor<R> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditor.class);

	private static final ValueSupplier INITIAL_VALUE = new InitialValue();

	private final Map<Attribute<?>, Event<?>> editEvents = new HashMap<>();
	private final DefaultPersistEvents persistEvents = new DefaultPersistEvents();
	private final EditorValues values = new DefaultEditorValues();
	private final Event<Attribute<?>> valueChanged = Event.event();

	private final Map<Attribute<?>, EditorValue<?>> editorValues = new HashMap<>();
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
	private final DefaultSettings settings;
	private final State primaryKeyPresent = State.state(false);
	private final State valid = State.state();
	private final DefaultExists exists;
	private final DefaultModified modified;
	private final DefaultPresent present = new DefaultPresent();
	private final Value<EntityValidator> validator;
	private final ComponentModels componentModels;
	private final SearchModels searchModels = new DefaultSearchModels();
	private final DefaultDetailEditors detail = new DefaultDetailEditors();
	private final EditorPersistence persistence;
	private final DefaultEditorEntity entity;

	/**
	 * Instantiates a new {@link DefaultEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 * @param componentModels the editor component models
	 */
	public DefaultEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider,
														 ComponentModels componentModels) {
		this.entityDefinition = requireNonNull(connectionProvider).entities().definition(entityType);
		this.connectionProvider = requireNonNull(connectionProvider);
		this.settings = new DefaultSettings(entityDefinition.readOnly());
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
		updateStates();
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
	public final Settings settings() {
		return settings;
	}

	@Override
	public final PersistEvents events() {
		return persistEvents;
	}

	@Override
	public final EditorEntity entity() {
		return entity;
	}

	@Override
	public final EditorValues values() {
		return values;
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
	public final Present present() {
		return present;
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
	public final DetailEditors<R> detail() {
		return detail;
	}

	@Override
	public final EditorPersistence persistence() {
		return persistence;
	}

	@Override
	public final EditorTasks tasks() {
		return tasks(connectionProvider.connection());
	}

	@Override
	public final EditorTasks tasks(EntityConnection connection) {
		return new DefaultEditorTasks(requireNonNull(connection));
	}

	@Override
	public final Entity insert() throws EntityValidationException {
		return tasks().insert().perform().handle();
	}

	@Override
	public final Collection<Entity> insert(Collection<Entity> entities) throws EntityValidationException {
		return tasks().insert(entities).perform().handle();
	}

	@Override
	public final Entity update() throws EntityValidationException {
		return tasks().update().perform().handle();
	}

	@Override
	public final Collection<Entity> update(Collection<Entity> entities) throws EntityValidationException {
		return tasks().update(entities).perform().handle();
	}

	@Override
	public final Entity delete() {
		return tasks().delete().perform().handle();
	}

	@Override
	public final Collection<Entity> delete(Collection<Entity> entities) {
		return tasks().delete(entities).perform().handle();
	}

	@Override
	public final String toString() {
		return getClass() + ", " + entityDefinition.type();
	}

	/**
	 * @return the {@link ComponentModels} instance
	 */
	protected ComponentModels componentModels() {
		return componentModels;
	}

	private void notifyValueChange(Attribute<?> attribute, Map<Attribute<?>, String> invalidAttributes) {
		updateAttributeStates(attribute, invalidAttributes);
		DefaultEditorValue<?> editorValue = (DefaultEditorValue<?>) editorValues.get(attribute);
		if (editorValue != null) {
			editorValue.valueChanged();
		}
		valueChanged.accept(attribute);
	}

	private Map<Attribute<?>, String> updateStates() {
		Entity instance = entity.get();
		exists.update(instance);
		present.update(instance);
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
			modifiedState.set(entity.instance.modified(attribute));
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
		private final Event<Entity> replaced = Event.event();

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
			setDetail(entity);
			setOrDefaults(entity);
			changed.accept(entity);
		}

		@Override
		public Observer<Entity> changing() {
			return changing.observer();
		}

		@Override
		public Observer<Entity> replaced() {
			return replaced.observer();
		}

		@Override
		public void replace(@Nullable Entity entity) {
			Entity validated = entity == null ? null : validateType(entity);
			if (identityChanged(validated)) {
				replaceDetail(validated);
			}
			setOrDefaults(validated);
			replaced.accept(entity);
		}

		@Override
		public void refresh() {
			tasks().refresh().perform().handle();
		}

		private void setDetail(@Nullable Entity entity) {
			setDetail(entity, connectionProvider.connection());
		}

		private void replaceDetail(@Nullable Entity entity) {
			replaceDetail(entity, connectionProvider.connection());
		}

		private void setDetail(@Nullable Entity entity, EntityConnection connection) {
			for (DetailEditor detail : detail.editors.values()) {
				detail.editor().entity().set(entity == null ? null : detail.link.load(entity, connection));
			}
		}

		private void replaceDetail(@Nullable Entity entity, EntityConnection connection) {
			for (DetailEditor detail : detail.editors.values()) {
				detail.editor().entity().replace(entity == null ? null : detail.link.load(entity, connection));
			}
		}

		private void setOrDefaults(@Nullable Entity entity) {
			Map<Attribute<?>, Object> affectedAttributes = instance.set(entity == null ? createEntity(this::defaultValue) : entity);
			Map<Attribute<?>, String> invalidAttributes = updateStates();
			for (Attribute<?> affectedAttribute : affectedAttributes.keySet()) {
				notifyValueChange(affectedAttribute, invalidAttributes);
			}
			attributeModified.forEach((attribute, modifiedState) -> modifiedState.set(instance.modified(attribute)));
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
			if (entity != null && !entityDefinition.type().equals(entity.type())) {
				throw new IllegalStateException("Entity type mismatch for entity: " + entity);
			}

			return entity;
		}

		private boolean identityChanged(@Nullable Entity newEntity) {
			if (newEntity == null) {
				return !instance.primaryKey().isNull();
			}

			return !instance.primaryKey().equals(newEntity.primaryKey());
		}
	}

	private final class DefaultEditorTasks implements EditorTasks {

		private static final String NOT_MODIFIED = "Entity is not modified: ";
		private static final String INSERT = "insert {}";
		private static final String UPDATE = "update {}";
		private static final String DELETE = "delete {}";

		private final EntityConnection connection;

		private DefaultEditorTasks(EntityConnection connection) {
			this.connection = connection;
		}

		@Override
		public PersistTask<Entity> insert() throws EntityValidationException {
			return insert(e -> {});
		}

		@Override
		public PersistTask<Entity> insert(Consumer<Entity> before) throws EntityValidationException {
			settings.verifyInsertEnabled();
			validate();
			Entity entity = entity().get().copy().mutable();
			persistEvents.beforeInsert(entity);

			return detail.editors.isEmpty() ? new InsertEditor(entity, before) : new InsertDetail(entity, before);
		}

		@Override
		public PersistTask<Entity> insert(Entity entity) throws EntityValidationException {
			settings.verifyInsertEnabled();
			validate(requireNonNull(entity));
			entity = entity.copy().mutable();
			persistEvents.beforeInsert(entity);

			return new InsertEntity(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> insert(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyInsertEnabled();
			validate(requireNonNull(entities));
			entities = unmodifiableCollection(new ArrayList<>(entities));
			persistEvents.beforeInsert(entities);

			return new InsertEntities(entities);
		}

		@Override
		public PersistTask<Entity> update() throws EntityValidationException {
			settings.verifyUpdateEnabled(1);
			validate();
			if (!modified().is()) {
				throw new IllegalStateException(NOT_MODIFIED + entity().get());
			}
			Entity entity = entity().get().copy().mutable();
			persistEvents.beforeUpdate(entity);

			return detail.editors.isEmpty() ? new UpdateEditor(entity) : new UpdateDetail(entity);
		}

		@Override
		public PersistTask<Entity> update(Entity entity) throws EntityValidationException {
			settings.verifyUpdateEnabled(1);
			validate(requireNonNull(entity));
			if (!entity.modified()) {
				throw new IllegalStateException(NOT_MODIFIED + entity);
			}
			entity = entity.copy().mutable();
			persistEvents.beforeUpdate(entity);

			return new UpdateEntity(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> update(Collection<Entity> entities) throws EntityValidationException {
			settings.verifyUpdateEnabled(requireNonNull(entities).size());
			validate(entities);
			for (Entity entity : entities) {
				if (!entity.modified()) {
					throw new IllegalStateException(NOT_MODIFIED + entity);
				}
			}
			entities = unmodifiableCollection(new ArrayList<>(entities));
			persistEvents.beforeUpdate(entities);

			return new UpdateEntities(entities);
		}

		@Override
		public PersistTask<Entity> delete() {
			settings.verifyDeleteEnabled();
			Entity entity = entity().get().copy().mutable();
			entity.revert();// in case of a modified primary key
			persistEvents.beforeDelete(entity);

			return detail.editors.isEmpty() ? new DeleteEditor(entity) : new DeleteDetail(entity);
		}

		@Override
		public PersistTask<Entity> delete(Entity entity) {
			settings.verifyDeleteEnabled();
			entity = requireNonNull(entity).copy().mutable();
			entity.revert();// in case of a modified primary key
			persistEvents.beforeDelete(entity);

			return new DeleteEntity(entity);
		}

		@Override
		public PersistTask<Collection<Entity>> delete(Collection<Entity> entities) {
			settings.verifyDeleteEnabled();
			entities = unmodifiableCollection(new ArrayList<>(requireNonNull(entities)));
			entities.forEach(Entity::revert);// in case of a modified primary key
			persistEvents.beforeDelete(entities);

			return new DeleteEntities(entities);
		}

		@Override
		public RefreshTask refresh() {
			return new DefaultRefreshTask(entity.instance.copy().builder()
								.original() // in case of modifications
								.build());
		}

		private final class InsertDetail implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<PersistTask<Entity>> tasks = new ArrayList<>(detail.editors.size());
			private final Consumer<Entity> before;

			private @Nullable Entity inserted;

			private InsertDetail(Entity entity, Consumer<Entity> before) throws EntityValidationException {
				this.entity = entity;
				this.before = requireNonNull(before);
				for (DetailEditor detailEditor : detail.editors.values()) {
					if (detailEditor.editor.present().is()) {
						tasks.add(detailEditor.editor.tasks(connection).insert(detail ->
										detailEditor.link.beforeInsert(detail, inserted(), connection)));
					}
				}
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(INSERT, entity);
				return transaction(connection, this::insert);
			}

			private Result<Entity> insert() {
				before.accept(entity);
				inserted = persistence.get().insert(entity, connection);
				Collection<Result<Entity>> detail = detail();
				return () -> {
					detail.forEach(Result::handle);
					entity().replace(inserted);
					persistEvents.afterInsert(inserted);

					return inserted;
				};
			}

			private Collection<Result<Entity>> detail() {
				return tasks.stream()
								.map(PersistTask::perform)
								.collect(toList());
			}

			private @Nullable Entity inserted() {
				return inserted;
			}
		}

		private final class UpdateDetail implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<PersistTask<Entity>> tasks = new ArrayList<>(detail.editors.size());

			private @Nullable Entity updated;

			private UpdateDetail(Entity entity) throws EntityValidationException {
				this.entity = entity;
				for (DetailEditor detailEditor : detail.editors.values()) {
					if (detailEditor.updatable.insert.is()) {
						tasks.add(detailEditor.editor.tasks(connection).insert(detail ->
										detailEditor.link.beforeInsert(detail, updated(), connection)));
					}
					else if (detailEditor.updatable.update.is()) {
						tasks.add(detailEditor.editor.tasks(connection).update());
					}
					else if (detailEditor.updatable.delete.is()) {
						tasks.add(detailEditor.editor.tasks(connection).delete());
					}
				}
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(UPDATE, entity);
				return transaction(connection, this::update);
			}

			private Result<Entity> update() {
				updated = entity;
				if (entity.modified()) {
					updated = persistence.get().update(entity, connection);
				}
				Collection<Result<Entity>> detail = detail();
				return () -> {
					detail.forEach(Result::handle);
					entity().replace(updated);
					persistEvents.afterUpdate(entity, updated);

					return updated;
				};
			}

			private Collection<Result<Entity>> detail() {
				return tasks.stream()
								.map(PersistTask::perform)
								.collect(toList());
			}

			private @Nullable Entity updated() {
				return updated;
			}
		}

		private final class DeleteDetail implements PersistTask<Entity> {

			private final Entity entity;
			private final Collection<PersistTask<Entity>> tasks = new ArrayList<>(detail.editors.size());

			private DeleteDetail(Entity entity) {
				this.entity = entity;
				for (DetailEditor detailEditor : detail.editors.values()) {
					if (detailEditor.editor.exists().is()) {
						tasks.add(detailEditor.editor.tasks(connection).delete());
					}
				}
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(DELETE, entity);
				return transaction(connection, this::delete);
			}

			private Result<Entity> delete() {
				Collection<Result<Entity>> detail = detail();
				persistence.get().delete(entity, connection);
				return () -> {
					detail.forEach(Result::handle);
					entity().replace(null);
					persistEvents.afterDelete(entity);

					return entity;
				};
			}

			private Collection<Result<Entity>> detail() {
				return tasks.stream()
								.map(PersistTask::perform)
								.collect(toList());
			}
		}

		private final class InsertEditor implements PersistTask<Entity> {

			private final Entity entity;
			private final Consumer<Entity> before;

			private InsertEditor(Entity entity, Consumer<Entity> before) {
				this.entity = entity;
				this.before = requireNonNull(before);
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(INSERT, entity);
				before.accept(entity);
				Entity inserted = persistence.get().insert(entity, connection);
				return () -> {
					entity().replace(inserted);
					persistEvents.afterInsert(inserted);

					return inserted;
				};
			}
		}

		private final class UpdateEditor implements PersistTask<Entity> {

			private final Entity entity;

			private UpdateEditor(Entity entity) {
				this.entity = entity;
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(UPDATE, entity);
				Entity updated = persistence.get().update(entity, connection);
				return () -> {
					entity().replace(updated);
					persistEvents.afterUpdate(entity, updated);

					return updated;
				};
			}
		}

		private final class DeleteEditor implements PersistTask<Entity> {

			private final Entity entity;

			private DeleteEditor(Entity entity) {
				this.entity = entity;
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(DELETE, entity);
				persistence.get().delete(entity, connection);
				return () -> {
					entity().replace(null);
					persistEvents.afterDelete(entity);

					return entity;
				};
			}
		}

		private final class InsertEntity implements PersistTask<Entity> {

			private final Entity entity;

			private InsertEntity(Entity entity) {
				this.entity = entity;
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(INSERT, entity);
				Entity inserted = persistence.get().insert(entity, connection);
				return () -> {
					persistEvents.afterInsert(inserted);

					return inserted;
				};
			}
		}

		private final class InsertEntities implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;

			private InsertEntities(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public Result<Collection<Entity>> perform() {
				LOG.debug(INSERT, entities);
				Collection<Entity> inserted = persistence.get().insert(entities, connection);
				return () -> {
					persistEvents.afterInsert(inserted);

					return inserted;
				};
			}
		}

		private final class UpdateEntity implements PersistTask<Entity> {

			private final Entity entity;

			private UpdateEntity(Entity entity) {
				this.entity = entity;
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(UPDATE, entity);
				Entity updated = persistence.get().update(entity, connection);
				return () -> {
					persistEvents.afterUpdate(entity, updated);

					return updated;
				};
			}
		}

		private final class UpdateEntities implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;

			private UpdateEntities(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public Result<Collection<Entity>> perform() {
				LOG.debug(UPDATE, entities);
				Collection<Entity> updated = persistence.get().update(entities, connection);
				return () -> {
					persistEvents.afterUpdate(originalEntityMap(entities, updated));

					return updated;
				};
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

			private DeleteEntity(Entity entity) {
				this.entity = entity;
			}

			@Override
			public Result<Entity> perform() {
				LOG.debug(DELETE, entity);
				persistence.get().delete(entity, connection);
				return () -> {
					persistEvents.afterDelete(entity);

					return entity;
				};
			}
		}

		private final class DeleteEntities implements PersistTask<Collection<Entity>> {

			private final Collection<Entity> entities;

			private DeleteEntities(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public Result<Collection<Entity>> perform() {
				LOG.debug(DELETE, entities);
				persistence.get().delete(entities, connection);
				return () -> {
					persistEvents.afterDelete(entities);

					return entities;
				};
			}
		}

		private final class DefaultRefreshTask implements RefreshTask {

			private final Entity original;

			private DefaultRefreshTask(Entity original) {
				this.original = original;
			}

			@Override
			public Result perform() {
				if (exists().is()) {
					Entity selected = connection.selectSingle(where(key(original.primaryKey()))
									.include(entityDefinition.columns().definitions().stream()
													.filter(definition -> !definition.selected())
													.map(ColumnDefinition::attribute)
													.filter(entity.instance::contains)
													.collect(toSet()))
									.build())
									.immutable();// passed on to detail Select providers
					return () -> {
						entity.setDetail(selected);
						entity.set(selected);
					};
				}

				return () -> {};
			}
		}
	}

	final class DefaultDetailEditors implements DetailEditors<R> {

		private final Map<LinkKey, DetailEditor> editors = new LinkedHashMap<>();

		@Override
		public void add(EditorLink editorLink) {
			requireNonNull(editorLink);
			DefaultEditorLink<R> link = (DefaultEditorLink<R>) editorLink;
			ForeignKey foreignKey = null;
			if (link instanceof ForeignKeyEditorLink) {
				foreignKey = ((DefaultForeignKeyLink<R>) link).foreignKey();
				if (!entityDefinition.type().equals(foreignKey.referencedType())) {
					throw new IllegalArgumentException("Master editor entity type " + foreignKey.referencedType() +
									" expected, got: " + entityDefinition.type());
				}
				if (!foreignKey.entityType().equals(link.editor().entityDefinition().type())) {
					throw new IllegalArgumentException("Detail editor entity type " + foreignKey.entityType() +
									" expected, got: " + link.editor().entityDefinition().type());
				}
			}
			LinkKey key = new LinkKey(foreignKey, link.name());
			if (editors.containsKey(key)) {
				throw new IllegalArgumentException("Detail editor already exists for " + key);
			}
			editors.put(key, new DetailEditor(link));
		}

		@Override
		public R get(ForeignKey foreignKey) {
			return findByForeignKey(foreignKey).editor();
		}

		@Override
		public R get(String name) {
			return findByName(name).editor();
		}

		@Override
		public String caption(String name) {
			return findByName(name).link.caption();
		}

		@Override
		public String caption(ForeignKey foreignKey) {
			return findByForeignKey(foreignKey).link.caption();
		}

		String name(ForeignKey foreignKey) {
			return findByForeignKey(foreignKey).link.name();
		}

		private DetailEditor findByForeignKey(ForeignKey foreignKey) {
			requireNonNull(foreignKey);
			List<DetailEditor> matches = editors.entrySet().stream()
							.filter(entry -> foreignKey.equals(entry.getKey().foreignKey))
							.map(Map.Entry::getValue)
							.collect(toList());
			if (matches.isEmpty()) {
				throw new IllegalStateException("No detail editor is associated with foreign key: " + foreignKey);
			}
			if (matches.size() > 1) {
				throw new IllegalStateException("Multiple detail editors are associated with foreign key " +
								foreignKey + "; use get(String) with one of: " + matches.stream()
								.map(detail -> detail.link.name())
								.collect(toList()));
			}

			return matches.get(0);
		}

		private DetailEditor findByName(String name) {
			requireNonNull(name);
			List<DetailEditor> matches = editors.entrySet().stream()
							.filter(entry -> entry.getKey().name.equals(name))
							.map(Map.Entry::getValue)
							.collect(toList());
			if (matches.isEmpty()) {
				throw new IllegalStateException("No detail editor is associated with the given name: " + name);
			}
			if (matches.size() > 1) {
				throw new IllegalStateException("Multiple detail editors share the name " + name +
								"; use get(ForeignKey) with one of: " + matches.stream()
												.map(detail -> detail.link instanceof ForeignKeyEditorLink
																? ((DefaultForeignKeyLink<R>) detail.link).foreignKey().toString()
																: "(non-FK link)")
												.collect(toList()));
			}

			return matches.get(0);
		}
	}

	private static final class LinkKey {

		private final @Nullable ForeignKey foreignKey;
		private final String name;

		private LinkKey(@Nullable ForeignKey foreignKey, String name) {
			this.foreignKey = foreignKey;
			this.name = name;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof DefaultEntityEditor.LinkKey)) {
				return false;
			}
			LinkKey key = (LinkKey) object;

			return Objects.equals(foreignKey, key.foreignKey) && name.equals(key.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(foreignKey, name);
		}

		@Override
		public String toString() {
			return foreignKey == null ? "name=" + name : "foreignKey=" + foreignKey + ", name=" + name;
		}
	}

	static sealed class DefaultEditorLink<R extends EntityEditor<R>> implements EditorLink {

		private final R editor;
		private final String name;
		private final String caption;
		private final Predicate<Entity> present;
		private final DetailEntity entity;
		private final BeforeInsert beforeInsert;

		private DefaultEditorLink(DefaultEditorLinkBuilder<R> builder) {
			this.editor = builder.editor;
			this.name = builder.name;
			this.caption = builder.caption == null ? builder.name : builder.caption;
			this.present = builder.present;
			this.entity = builder.entity;
			this.beforeInsert = builder.beforeInsert;
			this.editor.present().predicate().set(builder.present);
			this.editor.present().predicate().addValidator(new PreventPresentModification());
		}

		private final R editor() {
			return editor;
		}

		private final String name() {
			return name;
		}

		private final String caption() {
			return caption;
		}

		private final @Nullable Entity load(Entity master, EntityConnection connection) {
			return entity.detail(master, connection);
		}

		private final void beforeInsert(Entity detail, Entity master, EntityConnection connection) {
			beforeInsert.apply(requireNonNull(detail), requireNonNull(master), requireNonNull(connection));
		}

		private class PreventPresentModification implements Validator<Predicate<Entity>> {

			@Override
			public void validate(Predicate<Entity> value) {
				if (value != present) {
					throw new IllegalStateException("Detail editor present predicate cannot be replaced after registration; " +
									"supply it via EditorLink.Builder.present() before calling DetailEditors.add()");
				}
			}
		}
	}

	static final class DefaultForeignKeyLink<R extends EntityEditor<R>>
					extends DefaultEditorLink<R> implements ForeignKeyEditorLink {

		private final ForeignKey foreignKey;

		private DefaultForeignKeyLink(DefaultEditorLinkBuilder<R> builder, ForeignKey foreignKey) {
			super(builder);
			this.foreignKey = foreignKey;
		}

		private ForeignKey foreignKey() {
			return foreignKey;
		}
	}

	static class DefaultEditorLinkBuilder<R extends EntityEditor<R>>
					implements EditorLink.Builder<R> {

		static final EditorStep EDITOR = new DefaultEditorStep();

		private final R editor;

		protected String name;
		protected DetailEntity entity;
		protected @Nullable String caption;
		private Predicate<Entity> present = entity -> true;
		private BeforeInsert beforeInsert = (d, m, connection) -> {};

		private DefaultEditorLinkBuilder(R editor, String name, DetailEntity entity) {
			this.editor = editor;
			this.name = name;
			this.entity = entity;
		}

		@Override
		public EditorLink.Builder<R> present(Predicate<Entity> present) {
			this.present = requireNonNull(present);
			return this;
		}

		@Override
		public EditorLink.Builder<R> beforeInsert(BeforeInsert beforeInsert) {
			this.beforeInsert = requireNonNull(beforeInsert);
			return this;
		}

		@Override
		public EditorLink.Builder<R> caption(String caption) {
			if (nullOrEmpty(caption)) {
				throw new IllegalArgumentException("caption must be a non-empty string");
			}
			this.caption = caption;
			return this;
		}

		@Override
		public EditorLink build() {
			return new DefaultEditorLink<>(this);
		}

		static final class DefaultEditorStep implements EditorStep {

			@Override
			public <R extends EntityEditor<R>> ForeignKeyNameStep<R> editor(R editor) {
				return new DefaultForeignKeyNameStep<>(requireNonNull(editor));
			}
		}

		private static final class DefaultForeignKeyNameStep<R extends EntityEditor<R>>
						implements ForeignKeyNameStep<R> {

			private final R editor;

			private DefaultForeignKeyNameStep(R editor) {
				this.editor = editor;
			}

			@Override
			public ForeignKeyEditorLink.Builder<R> foreignKey(ForeignKey foreignKey) {
				return new DefaultForeignKeyEditorLinkBuilder<>(editor,
								requireNonNull(foreignKey).name(), foreignKey, new DefaultDetailEntity(master -> Select.where(foreignKey.equalTo(master)).build()))
								.beforeInsert((detail, master, connection) -> detail.set(foreignKey, master));
			}

			@Override
			public LoadStep<R> name(String name) {
				if (nullOrEmpty(name)) {
					throw new IllegalArgumentException("Detail editor name can't be empty");
				}
				return new DefaultLoadStep<>(editor, name);
			}
		}

		private static final class DefaultLoadStep<R extends EntityEditor<R>> implements LoadStep<R> {

			private final R editor;
			private final String name;

			private DefaultLoadStep(R editor, String name) {
				this.editor = editor;
				this.name = name;
			}

			@Override
			public EditorLink.Builder<R> condition(DetailCondition condition) {
				requireNonNull(condition);
				return select(master -> Select.where(condition.get(master)).build());
			}

			@Override
			public EditorLink.Builder<R> select(DetailSelect select) {
				requireNonNull(select);
				return new DefaultEditorLinkBuilder<>(editor, name, new DefaultDetailEntity(select));
			}

			@Override
			public EditorLink.Builder<R> entity(DetailEntity entity) {
				return new DefaultEditorLinkBuilder<>(editor, name, requireNonNull(entity));
			}
		}
	}

	private static final class DefaultForeignKeyEditorLinkBuilder<R extends EntityEditor<R>>
					extends DefaultEditorLinkBuilder<R> implements ForeignKeyEditorLink.Builder<R> {

		private final ForeignKey foreignKey;

		private DefaultForeignKeyEditorLinkBuilder(R editor, String name, ForeignKey foreignKey, DetailEntity entity) {
			super(editor, name, entity);
			this.foreignKey = foreignKey;
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> name(String name) {
			if (nullOrEmpty(name)) {
				throw new IllegalArgumentException("Detail editor name can't be empty");
			}
			this.name = name;
			return this;
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> condition(DetailCondition condition) {
			requireNonNull(condition);
			return select(master -> Select.where(condition.get(master)).build());
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> select(DetailSelect select) {
			requireNonNull(select);
			return entity(new DefaultDetailEntity(select));
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> entity(DetailEntity entity) {
			this.entity = requireNonNull(entity);
			return this;
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> present(Predicate<Entity> present) {
			super.present(present);
			return this;
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> beforeInsert(BeforeInsert beforeInsert) {
			super.beforeInsert(beforeInsert);
			return this;
		}

		@Override
		public ForeignKeyEditorLink.Builder<R> caption(String caption) {
			super.caption(caption);
			return this;
		}

		@Override
		public ForeignKeyEditorLink build() {
			return new DefaultForeignKeyLink<>(this, foreignKey);
		}
	}

	private static final class DefaultDetailEntity implements DetailEntity {

		private final DetailSelect select;

		private DefaultDetailEntity(DetailSelect select) {
			this.select = select;
		}

		@Override
		public @Nullable Entity detail(Entity master, EntityConnection connection) {
			List<Entity> entities = requireNonNull(connection).select(select.get(requireNonNull(master)));
			if (entities.size() > 1) {
				throw new IllegalStateException("Multiple detail rows found for master entity " + master +
								", detail editors require 0-or-1 cardinality");
			}

			return entities.isEmpty() ? null : entities.get(0);
		}
	}

	private final class DetailEditor {

		private final R editor;
		private final DefaultEditorLink<R> link;
		private final Updatable updatable = new Updatable();

		private DetailEditor(DefaultEditorLink<R> link) {
			this.editor = link.editor();
			this.link = link;
			if (link instanceof ForeignKeyEditorLink) {
				ForeignKey foreignKey = ((DefaultForeignKeyLink<R>) link).foreignKey();
				editor.validator().update(validator -> new DetailForeignKeyValidator(validator, foreignKey));
				editor.validator().addValidator(new PreventValidatorModification());
				editor.value(foreignKey).persist().set(false);
			}
			editor.values().changed().addListener(this::refreshStates);
			editor.entity().replaced().addListener(this::refreshStates);
			editor.modified().addListener(this::refreshStates);
			modified().additional().add(updatable);
		}

		private R editor() {
			return editor;
		}

		private void refreshStates() {
			boolean masterExists = exists().is();
			boolean exists = editor.exists().is();
			boolean modified = editor.modified().is();
			updatable.insert.set(masterExists && editor.present().is() && !exists);
			updatable.update.set(masterExists && editor.present().is() && exists && modified);
			updatable.delete.set(masterExists && !editor.present().is() && exists);
			DefaultEntityEditor.this.modified.update();
		}

		private final class Updatable implements ObservableState {

			private final State insert = State.state();
			private final State update = State.state();
			private final State delete = State.state();

			private final ObservableState state = State.or(insert, update, delete);

			@Override
			public boolean is() {
				return state.is();
			}

			@Override
			public ObservableState not() {
				return state.not();
			}

			@Override
			public Observer<Boolean> observer() {
				return state.observer();
			}
		}
	}

	private static final class PreventValidatorModification implements Validator<EntityValidator> {

		@Override
		public void validate(@Nullable EntityValidator validator) {
			if (!(validator instanceof DetailForeignKeyValidator)) {
				throw new IllegalArgumentException("Detail editor validator cannot be replaced after registration; " +
								"set a custom validator before calling DetailEditors.add()");
			}
		}
	}

	static final class DetailForeignKeyValidator implements EntityValidator {

		private final EntityValidator validator;
		private final ForeignKey foreignKey;

		private DetailForeignKeyValidator(EntityValidator validator, ForeignKey foreignKey) {
			this.validator = validator;
			this.foreignKey = foreignKey;
		}

		@Override
		public boolean nullable(Entity entity, Attribute<?> attribute) {
			return validator.nullable(entity, attribute);
		}

		@Override
		public void validate(Entity entity, Attribute<?> attribute) throws AttributeValidationException {
			try {
				validator.validate(entity, attribute);
			}
			catch (AttributeValidationException e) {
				if (notExcluded(e)) {
					throw e;
				}
			}
		}

		@Override
		public void validate(Entity entity) throws EntityValidationException {
			try {
				validator.validate(entity);
			}
			catch (EntityValidationException e) {
				if (e.attributes().isEmpty()) {
					throw e;
				}
				List<AttributeValidationException> filtered = e.attributes().stream()
								.filter(this::notExcluded)
								.collect(toList());
				if (!filtered.isEmpty()) {
					throw new EntityValidationException(filtered);
				}
			}
		}

		@Override
		public boolean validated(Entity entity, AttributeDefinition<?> definition) {
			return validator.validated(entity, definition);
		}

		@Override
		public boolean strict() {
			return validator.strict();
		}

		private boolean notExcluded(AttributeValidationException e) {
			if (!(e instanceof NullValueException)) {
				return true;
			}
			Attribute<?> attribute = e.attribute();
			if (attribute.equals(foreignKey)) {
				return false;
			}
			return foreignKey.references().stream()
							.map(ForeignKey.Reference::column)
							.noneMatch(column -> column.equals(attribute));
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

	private final class DefaultEditorValues implements EditorValues {

		@Override
		public void clear() {
			entity.set(entityDefinition.entity());
		}

		@Override
		public void defaults() {
			entity.set(null);
		}

		@Override
		public void revert() {
			entityDefinition.attributes().get().forEach(attribute -> value(attribute).revert());
			for (DetailEditor detail : detail.editors.values()) {
				detail.editor().values().revert();
			}
		}

		@Override
		public Observer<Attribute<?>> changed() {
			return valueChanged.observer();
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
			attributes.set(modifiedAttributes());
			modified.set(exists.is() && entity.instance.modified() || additional.get().stream()
							.anyMatch(ObservableState::is));
		}

		private Set<Attribute<?>> modifiedAttributes() {
			Set<Attribute<?>> modifiedAttributes = new HashSet<>(editorValues.keySet().stream()
							.filter(entity.instance::modified)
							.collect(toSet()));
			detail.editors.values().stream()
							.map(editor -> editor.editor.modified().attributes().get())
							.forEach(modifiedAttributes::addAll);

			return modifiedAttributes;
		}

		private void additionalChanged(Change<Set<ObservableState>> change) {
			change.previous().forEach(state -> state.removeListener(additionalListener));
			change.current().forEach(state -> state.addListener(additionalListener));
			update();
		}
	}

	private final class DefaultPresent implements Present {

		private final Value<Predicate<Entity>> predicate = Value.builder()
						.nonNull((Predicate<Entity>) entity -> true)
						.listener(this::update)
						.build();

		private final State state = State.state();

		@Override
		public Value<Predicate<Entity>> predicate() {
			return predicate;
		}

		@Override
		public boolean is() {
			return state.is();
		}

		@Override
		public ObservableState not() {
			return state.not();
		}

		@Override
		public Observer<Boolean> observer() {
			return state.observer();
		}

		private void update() {
			update(entity.get());
		}

		private void update(Entity entity) {
			state.set(predicate.get().test(entity));
		}
	}

	private final class DefaultEditorValue<T> extends AbstractValue<T> implements EditorValue<T> {

		private final Attribute<T> attribute;
		private final Value<Supplier<T>> defaultValue;
		private final Map<Attribute<?>, Function<@Nullable T, ?>> propagators = new HashMap<>();

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
							k -> State.state(entity.instance.modified(attribute))).observable();
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
		public <V> void propagate(Attribute<V> attribute, Function<@Nullable T, @Nullable V> deriver) {
			entityDefinition.attributes().definition(attribute);
			propagators.put(attribute, requireNonNull(deriver));
		}

		@Override
		public void set(Entity entity, @Nullable T value) {
			requireNonNull(entity).set(attribute, value);
			propagators.forEach((attr, deriver) ->
							entity.set((Attribute<Object>) attr, deriver.apply(entity.get(attribute))));
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
			propagators.forEach((attr, deriver) ->
							value((Attribute<Object>) attr).set(deriver.apply(entity.instance.get(attribute))));
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

	private final class DefaultPersistEvents implements PersistEvents {

		private final DefaultBeforePersist before = new DefaultBeforePersist();
		private final DefaultAfterPersist after = new DefaultAfterPersist();
		private final Event<Collection<Entity>> persisted = Event.event();

		@Override
		public BeforePersist before() {
			return before;
		}

		@Override
		public AfterPersist after() {
			return after;
		}

		@Override
		public Observer<Collection<Entity>> persisted() {
			return persisted.observer();
		}

		private void beforeInsert(Entity entity) {
			beforeInsert(singleton(entity));
		}

		private void beforeInsert(Collection<Entity> entities) {
			before.insert.accept(entities);
		}

		private void afterInsert(Entity entity) {
			afterInsert(singleton(entity));
		}

		private void afterInsert(Collection<Entity> inserted) {
			after.insert.accept(inserted);
			persisted.accept(inserted);
			if (settings.publishPersistenceEvents.is()) {
				notifyInserted(inserted);
			}
		}

		private void beforeUpdate(Entity entity) {
			beforeUpdate(singleton(entity));
		}

		private void beforeUpdate(Collection<Entity> entities) {
			before.update.accept(entities);
		}

		private void afterUpdate(Entity before, Entity after) {
			afterUpdate(singletonMap(before, after));
		}

		private void afterUpdate(Map<Entity, Entity> updated) {
			after.update.accept(updated);
			persisted.accept(updated.values());
			if (settings.publishPersistenceEvents.is()) {
				notifyUpdated(updated);
			}
		}

		private void beforeDelete(Entity entity) {
			beforeDelete(singleton(entity));
		}

		private void beforeDelete(Collection<Entity> entities) {
			before.delete.accept(entities);
		}

		private void afterDelete(Entity deleted) {
			afterDelete(singleton(deleted));
		}

		private void afterDelete(Collection<Entity> deleted) {
			after.delete.accept(deleted);
			persisted.accept(deleted);
			if (settings.publishPersistenceEvents.is()) {
				notifyDeleted(deleted);
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

		private static final class DefaultBeforePersist implements BeforePersist {

			private final Event<Collection<Entity>> insert = Event.event();
			private final Event<Collection<Entity>> update = Event.event();
			private final Event<Collection<Entity>> delete = Event.event();

			@Override
			public Observer<Collection<Entity>> insert() {
				return insert.observer();
			}

			@Override
			public Observer<Collection<Entity>> update() {
				return update.observer();
			}

			@Override
			public Observer<Collection<Entity>> delete() {
				return delete.observer();
			}
		}

		private static final class DefaultAfterPersist implements AfterPersist {

			private final Event<Collection<Entity>> insert = Event.event();
			private final Event<Map<Entity, Entity>> update = Event.event();
			private final Event<Collection<Entity>> delete = Event.event();

			@Override
			public Observer<Collection<Entity>> insert() {
				return insert.observer();
			}

			@Override
			public Observer<Map<Entity, Entity>> update() {
				return update.observer();
			}

			@Override
			public Observer<Collection<Entity>> delete() {
				return delete.observer();
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

		@Override
		public State publishPersistenceEvents() {
			return publishPersistenceEvents;
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
}
