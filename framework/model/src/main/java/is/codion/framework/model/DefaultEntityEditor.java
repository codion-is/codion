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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;
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

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultEntityEditor implements EntityEditor {

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
	private final State primaryKeyNull = State.state(true);
	private final State entityValid = State.state();
	private final DefaultExists exists;
	private final DefaultModified modified;
	private final Value<EntityValidator> validator;

	private final Entity entity;

	DefaultEntityEditor(EntityDefinition entityDefinition) {
		this.entityDefinition = requireNonNull(entityDefinition);
		this.entity = createEntity(AttributeDefinition::defaultValue);
		this.exists = new DefaultExists(entityDefinition);
		this.modified = new DefaultModified();
		this.validator = Value.builder()
						.nonNull(entityDefinition.validator())
						.listener(this::updateValidState)
						.build();
		configurePersistentForeignKeys();
	}

	@Override
	public void set(@Nullable Entity entity) {
		changing.accept(entity == null ? null : entity.immutable());
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
	public Observer<Entity> changing() {
		return changing.observer();
	}

	@Override
	public Observer<Attribute<?>> valueChanged() {
		return valueChanged.observer();
	}

	@Override
	public boolean nullable(Attribute<?> attribute) {
		return validator.getOrThrow().nullable(entity, attribute);
	}

	@Override
	public ObservableState primaryKeyNull() {
		return primaryKeyNull.observable();
	}

	@Override
	public Value<EntityValidator> validator() {
		return validator;
	}

	@Override
	public ObservableState valid() {
		return entityValid.observable();
	}

	@Override
	public void validate() {
		validate(entity);
	}

	@Override
	public void validate(Attribute<?> attribute) {
		validator.getOrThrow().validate(entity, attribute);
	}

	@Override
	public void validate(Collection<Entity> entities) {
		for (Entity entityToValidate : requireNonNull(entities)) {
			validate(entityToValidate);
		}
	}

	@Override
	public void validate(Entity entity) {
		if (entity.type().equals(entityDefinition.type())) {
			validator.getOrThrow().validate(entity);
		}
		else {
			entity.definition().validator().validate(entity);
		}
	}

	@Override
	public <T> EditorValue<T> value(Attribute<T> attribute) {
		return (EditorValue<T>) editorValues.computeIfAbsent(attribute, this::createEditorValue);
	}

	void setOrDefaults(@Nullable Entity entity) {
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
		updateValidState();
		updatePrimaryKeyNullState();
	}

	private <T> void updateAttributeStates(Attribute<T> attribute) {
		State presentState = attributePresent.get(attribute);
		if (presentState != null) {
			presentState.set(!entity.isNull(attribute));
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

	private void updateValidState() {
		entityValid.set(validator.getOrThrow().valid(entity));
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
	 * @see ColumnDefinition.Builder#withDefault(boolean)
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
						.filter(columnDefinition -> !columnDefinition.withDefault() || columnDefinition.hasDefaultValue())
						.map(columnDefinition -> (AttributeDefinition<Object>) columnDefinition)
						.forEach(attributeDefinition -> newEntity.set(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
	}

	private void addTransientValues(ValueSupplier valueSupplier, Entity newEntity) {
		entityDefinition.attributes().definitions().stream()
						.filter(TransientAttributeDefinition.class::isInstance)
						.filter(attributeDefinition -> !attributeDefinition.derived())
						.map(attributeDefinition -> (AttributeDefinition<Object>) attributeDefinition)
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

	private <T> DefaultEditorValue<?> createEditorValue(Attribute<T> attribute) {
		entityDefinition.attributes().definition(attribute);

		return new DefaultEditorValue<>(attribute);
	}

	private interface ValueSupplier {
		<T> @Nullable T get(AttributeDefinition<T> attributeDefinition);
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
		private final Value<Predicate<Entity>> predicate = Value.builder()
						.nonNull((Predicate<Entity>) Entity::modified)
						.listener(this::update)
						.build();

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
			modified.set(exists.predicate.getOrThrow().test(entity) && predicate.getOrThrow().test(entity));
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
			this.defaultValue = Value.nonNull(new Supplier<T>() {
				@Override
				public @Nullable T get() {
					return entityDefinition.attributes().definition(attribute).defaultValue();
				}
			});
		}

		@Override
		public void revert() {
			if (modified().is()) {
				super.set(entity.original(attribute));
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

		private void valueChanged() {
			notifyListeners();
		}
	}

	private static <T> @Nullable T nullValue(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition.attribute().type().isBoolean() && !attributeDefinition.nullable()) {
			return (T) Boolean.FALSE;
		}

		return null;
	}
}
