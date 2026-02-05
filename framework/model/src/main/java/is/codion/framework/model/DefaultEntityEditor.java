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
import is.codion.common.reactive.value.ValueSet;
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
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel.EditorValue;
import is.codion.framework.model.EntityEditModel.EntityEditor;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

public class DefaultEntityEditor implements EntityEditor {

	private static final ValueSupplier INITIAL_VALUE = new InitialValue();

	private final Map<Attribute<?>, Event<?>> editEvents = new HashMap<>();
	private final Event<Attribute<?>> valueChanged = Event.event();
	private final Event<Entity> changing = Event.event();
	private final Event<Entity> changed = Event.event();

	private final Map<Attribute<?>, DefaultEditorValue<?>> editorValues = new HashMap<>();
	private final Map<Attribute<?>, State> persistValues = new HashMap<>();
	private final Map<Attribute<?>, State> attributeModified = new HashMap<>();
	private final Map<Attribute<?>, State> attributePresent = new HashMap<>();
	private final Map<Attribute<?>, State> attributeValid = new HashMap<>();
	private final Map<Attribute<?>, Observable<String>> messages = new HashMap<>();

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final State primaryKeyNull = State.state(true);
	private final State entityValid = State.state();
	private final DefaultExists exists;
	private final DefaultModified modified;
	private final Value<EntityValidator> validator;
	private final EditorModels editorModels;
	private final SearchModels searchModels = new DefaultSearchModels();

	private final Entity entity;

	/**
	 * Instantiates a new {@link DefaultEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public DefaultEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(entityType, connectionProvider, new DefaultEditorModels());
	}

	/**
	 * Instantiates a new {@link DefaultEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 * @param editorModels the editor models
	 */
	public DefaultEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider, EditorModels editorModels) {
		this.entityDefinition = requireNonNull(connectionProvider).entities().definition(entityType);
		this.connectionProvider = requireNonNull(connectionProvider);
		this.editorModels = requireNonNull(editorModels);
		this.entity = createEntity(INITIAL_VALUE);
		this.exists = new DefaultExists(entityDefinition);
		this.modified = new DefaultModified();
		this.validator = Value.builder()
						.nonNull(entityDefinition.validator())
						.listener(this::updateValidStates)
						.build();
		configurePersistentForeignKeys();
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
	public final void set(@Nullable Entity entity) {
		entity = entity == null ? null : entity.immutable();
		changing.accept(entity);
		setOrDefaults(entity);
	}

	@Override
	public final Entity get() {
		return entity.immutable();
	}

	@Override
	public final void replace(Entity entity) {
		setOrDefaults(requireNonNull(entity));
	}

	@Override
	public final void clear() {
		set(entityDefinition.entity());
	}

	@Override
	public final Observer<Entity> observer() {
		return changed.observer();
	}

	@Override
	public final void defaults() {
		set(null);
	}

	@Override
	public final void revert() {
		entityDefinition.attributes().get().forEach(attribute -> value(attribute).revert());
	}

	@Override
	public final void refresh() {
		if (exists.is()) {
			set(connectionProvider.connection().selectSingle(where(key(entity.originalPrimaryKey()))
							.include(entityDefinition.columns().definitions().stream()
											.filter(definition -> !definition.selected())
											.map(ColumnDefinition::attribute)
											.filter(entity::contains)
											.collect(toSet()))
							.build()));
		}
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
	public final Observer<Entity> changing() {
		return changing.observer();
	}

	@Override
	public final Observer<Attribute<?>> valueChanged() {
		return valueChanged.observer();
	}

	@Override
	public final boolean nullable(Attribute<?> attribute) {
		return validator.getOrThrow().nullable(entity, attribute);
	}

	@Override
	public final ObservableState primaryKeyNull() {
		return primaryKeyNull.observable();
	}

	@Override
	public final Value<EntityValidator> validator() {
		return validator;
	}

	@Override
	public final ObservableState valid() {
		return entityValid.observable();
	}

	@Override
	public final void validate() {
		validate(entity);
	}

	@Override
	public final void validate(Attribute<?> attribute) {
		validator.getOrThrow().validate(entity, attribute);
	}

	@Override
	public final void validate(Collection<Entity> entities) {
		for (Entity entityToValidate : requireNonNull(entities)) {
			validate(entityToValidate);
		}
	}

	@Override
	public final void validate(Entity entity) {
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

	private final void setOrDefaults(@Nullable Entity entity) {
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

	/**
	 * @return the {@link EditorModels} instance
	 */
	protected EditorModels editorModels() {
		return editorModels;
	}

	private <T> @Nullable T defaultValue(AttributeDefinition<T> attributeDefinition) {
		if (value(attributeDefinition.attribute()).persist().is()) {
			if (attributeDefinition instanceof ForeignKeyDefinition) {
				return (T) entity.entity((ForeignKey) attributeDefinition.attribute());
			}

			return entity.get(attributeDefinition.attribute());
		}

		return value(attributeDefinition.attribute()).defaultValue().getOrThrow().get();
	}

	private <T> void notifyValueEdit(Attribute<T> attribute, @Nullable T value, Map<Attribute<?>, Object> dependingValues) {
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
		DefaultEditorValue<?> valueEditor = editorValues.get(attribute);
		if (valueEditor != null) {
			valueEditor.valueChanged();
		}
		valueChanged.accept(attribute);
	}

	private void updateStates() {
		exists.update();
		modified.update();
		updateEntityValidState();
		updatePrimaryKeyNullState();
	}

	private void updateAttributeStates(Attribute<?> attribute) {
		State presentState = attributePresent.get(attribute);
		if (presentState != null) {
			presentState.set(!entity.isNull(attribute));
		}
		State validState = attributeValid.get(attribute);
		if (validState != null) {
			updateAttributeValidState(attribute, validState);
		}
		State modifiedState = attributeModified.get(attribute);
		if (modifiedState != null) {
			updateAttributeModifiedState(attribute, modifiedState);
		}
	}

	private boolean isValid(Attribute<?> attribute) {
		try {
			validator.getOrThrow().validate(entity, attribute);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}

	private void updateAttributeModifiedState(Attribute<?> attribute, State modifiedState) {
		modifiedState.set(exists.predicate.getOrThrow().test(entity) && entity.modified(attribute));
	}

	private void updateEntityValidState() {
		entityValid.set(validator.getOrThrow().valid(entity));
	}

	private void updateValidStates() {
		updateEntityValidState();
		attributeValid.forEach(this::updateAttributeValidState);
	}

	private void updateAttributeValidState(Attribute<?> attribute, State state) {
		state.set(isValid(attribute));
	}

	private void updatePrimaryKeyNullState() {
		primaryKeyNull.set(entity.primaryKey().isNull());
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

	private interface ValueSupplier {
		<T> @Nullable T get(AttributeDefinition<T> attributeDefinition);
	}

	/**
	 * A default {@link EditorModels} implementation.
	 */
	public static class DefaultEditorModels implements EditorModels {

		@Override
		public EntitySearchModel createSearchModel(ForeignKey foreignKey, EntityEditor editor) {
			requireNonNull(editor).entityDefinition().foreignKeys().definition(foreignKey);
			Collection<Column<String>> searchable = editor.entities().definition(foreignKey.referencedType()).columns().searchable();
			if (searchable.isEmpty()) {
				throw new IllegalStateException("No searchable columns defined for entity: " + foreignKey.referencedType());
			}

			return EntitySearchModel.builder()
							.entityType(foreignKey.referencedType())
							.connectionProvider(editor.connectionProvider())
							.build();
		}
	}

	private final class DefaultSearchModels implements SearchModels {

		private final Map<ForeignKey, EntitySearchModel> searchModels = new HashMap<>();

		@Override
		public EntitySearchModel get(ForeignKey foreignKey) {
			entityDefinition().foreignKeys().definition(foreignKey);
			synchronized (searchModels) {
				// can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
				// box models, createSearchModel() may for example call this function
				// see javadoc: must not attempt to update any other mappings of this map
				EntitySearchModel entitySearchModel = searchModels.get(foreignKey);
				if (entitySearchModel == null) {
					entitySearchModel = create(foreignKey);
					editorModels.configure(foreignKey, entitySearchModel, DefaultEntityEditor.this);
					searchModels.put(foreignKey, entitySearchModel);
				}

				return entitySearchModel;
			}
		}

		@Override
		public EntitySearchModel create(ForeignKey foreignKey) {
			return editorModels.createSearchModel(foreignKey, DefaultEntityEditor.this);
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
			exists.set(predicate.getOrThrow().test(entity));
		}
	}

	private final class DefaultModified implements Modified {

		private final State modified = State.state();
		private final ValueSet<Attribute<?>> attributes = ValueSet.valueSet();
		private final Value<Predicate<Entity>> predicate = Value.builder()
						.nonNull((Predicate<Entity>) Entity::modified)
						.listener(this::update)
						.build();

		@Override
		public ObservableValueSet<Attribute<?>> attributes() {
			return attributes.observable();
		}

		@Override
		public Value<Predicate<Entity>> predicate() {
			return predicate;
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
		public void update() {
			boolean existing = exists.predicate.getOrThrow().test(entity);
			attributes.set(existing ? editorValues.keySet().stream()
							.filter(entity::modified)
							.collect(toSet()) : emptySet());
			modified.set(existing && predicate.getOrThrow().test(entity));
		}

		@Override
		public Observer<Boolean> observer() {
			return modified.observer();
		}
	}

	private final class DefaultEditorValue<T> extends AbstractValue<T> implements EditorValue<T> {

		private final Attribute<T> attribute;
		private final Value<Supplier<T>> defaultValue;

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
			return entity.original(attribute);
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
				updateAttributeValidState(attribute, validState);
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
							k -> State.state(!entity.isNull(attribute))).observable();
		}

		@Override
		public Observable<String> message() {
			return messages.computeIfAbsent(attribute,
							k -> observableMessage(attribute));
		}

		@Override
		public ObservableState modified() {
			return attributeModified.computeIfAbsent(attribute,
							k -> State.state(exists.is() && entity.modified(attribute))).observable();
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
		protected @Nullable T getValue() {
			return entity.get(attribute);
		}

		@Override
		protected void setValue(@Nullable T value) {
			Map<Attribute<?>, Object> dependingValues = dependingValues(attribute);
			T previousValue = entity.set(attribute, value);
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

		private void addDependingDerivedAttributes(Attribute<?> attribute, Map<Attribute<?>, @Nullable Object> dependingValues) {
			entityDefinition.attributes().derivedFrom(attribute).forEach(derivedAttribute -> {
				dependingValues.put(derivedAttribute, entity.get(derivedAttribute));
				addDependingDerivedAttributes(derivedAttribute, dependingValues);
			});
		}

		private void addDependingForeignKeys(Column<?> column, Map<Attribute<?>, @Nullable Object> dependingValues) {
			entityDefinition.foreignKeys().definitions(column).forEach(foreignKeyDefinition ->
							dependingValues.put(foreignKeyDefinition.attribute(), entity.get(foreignKeyDefinition.attribute())));
		}

		private void addDependingReferencedColumns(ForeignKey foreignKey, Map<Attribute<?>, @Nullable Object> dependingValues) {
			foreignKey.references().forEach(reference ->
							dependingValues.put(reference.column(), entity.get(reference.column())));
		}

		private Observable<String> observableMessage(Attribute<?> attribute) {
			String description = entityDefinition.attributes().definition(attribute).description().orElse(null);
			Value<String> toolTip = Value.nullable(createMessage(description));
			value(attribute).addListener(() -> toolTip.set(createMessage(description)));

			return toolTip.observable();
		}

		private @Nullable String createMessage(@Nullable String description) {
			return createToolTipText(validationString(), description);
		}

		private @Nullable String validationString() {
			try {
				DefaultEntityEditor.this.validate(attribute);

				return null;
			}
			catch (ValidationException e) {
				return e.getMessage();
			}
		}

		private @Nullable String createToolTipText(@Nullable String validationMessage, @Nullable String description) {
			if (nullOrEmpty(validationMessage)) {
				return description;
			}
			else if (nullOrEmpty(description)) {
				return validationMessage;
			}

			return Stream.of(validationMessage, description)
							.collect(joining("<br>", "<html>", "</html"));
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
