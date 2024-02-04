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
import is.codion.common.db.exception.UpdateException;
import is.codion.common.event.Event;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

  private static final String ENTITIES = "entities";

  private final Entity entity;
  private final EntityConnectionProvider connectionProvider;
  private final Map<ForeignKey, EntitySearchModel> entitySearchModels = new HashMap<>();
  private final Map<Attribute<?>, Value<?>> editModelValues = new ConcurrentHashMap<>();
  private final Map<Attribute<?>, State> persistValues = new ConcurrentHashMap<>();
  private final Map<Attribute<?>, Supplier<?>> defaultValueSuppliers = new ConcurrentHashMap<>();
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
    this.validator = Value.value(entityDefinition().validator(), entityDefinition().validator());
    this.modifiedPredicate = Value.value(Entity::modified, Entity::modified);
    this.existsPredicate = Value.value(entityDefinition().exists(), entityDefinition().exists());
    this.states.readOnly.set(entityDefinition().readOnly());
    this.events.bindEvents();
    configurePersistentForeignKeys();
    setEntity(defaultEntity(AttributeDefinition::defaultValue));
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
  public final <T> void setDefault(Attribute<T> attribute, Supplier<T> defaultValue) {
    entityDefinition().attributes().definition(attribute);
    defaultValueSuppliers.put(attribute, requireNonNull(defaultValue, "defaultValue"));
  }

  @Override
  public final State overwriteWarning() {
    return states.overwriteWarning;
  }

  @Override
  public final State editEvents() {
    return states.editEvents;
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
  public final StateObserver primaryKeyNull() {
    return states.primaryKeyNull.observer();
  }

  @Override
  public final void set(Entity entity) {
    if (setEntityAllowed()) {
      setEntity(entity);
    }
  }

  @Override
  public final void setDefaults() {
    if (setEntityAllowed()) {
      setEntity(null);
    }
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
  public final void replace(ForeignKey foreignKey, Collection<Entity> entities) {
    replaceForeignKey(requireNonNull(foreignKey), requireNonNull(entities));
  }

  @Override
  public final Entity entity() {
    return entity.immutable();
  }

  @Override
  public final Entity referencedEntity(ForeignKey foreignKey) {
    return entity.referencedEntity(foreignKey);
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
  public final <T> T get(Attribute<T> attribute) {
    return entity.get(attribute);
  }

  @Override
  public final <T> Optional<T> optional(Attribute<T> attribute) {
    return entity.optional(attribute);
  }

  @Override
  public final <T> T put(Attribute<T> attribute, T value) {
    entityDefinition().attributes().definition(attribute);
    Map<Attribute<?>, Object> dependingValues = dependingValues(attribute);
    T previousValue = entity.put(attribute, value);
    if (!Objects.equals(value, previousValue)) {
      events.notifyValueEdit(attribute, value, dependingValues);
    }

    return previousValue;
  }

  @Override
  public final <T> T remove(Attribute<T> attribute) {
    entityDefinition().attributes().definition(attribute);
    T value = null;
    if (entity.contains(attribute)) {
      Map<Attribute<?>, Object> dependingValues = dependingValues(attribute);
      value = entity.remove(attribute);
      events.notifyValueEdit(attribute, null, dependingValues);
    }

    return value;
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
  public final void validate(Collection<? extends Entity> entities) throws ValidationException {
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
    states.verifyInsertEnabled();
    Entity toInsert = entity.copy();
    if (entityDefinition().primaryKey().generated()) {
      toInsert.clearPrimaryKey();
    }
    Collection<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    Entity inserted = insertedEntities.iterator().next();
    setEntity(inserted);

    notifyAfterInsert(unmodifiableCollection(insertedEntities));

    return inserted;
  }

  @Override
  public final Collection<Entity> insert(Collection<? extends Entity> entities) throws DatabaseException, ValidationException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return emptyList();
    }
    states.verifyInsertEnabled();
    Collection<Entity> insertedEntities = insertEntities(entities);

    notifyAfterInsert(unmodifiableCollection(insertedEntities));

    return insertedEntities;
  }

  @Override
  public final Entity update() throws DatabaseException, ValidationException {
    Collection<Entity> updated = update(singletonList(entity.copy()));
    if (updated.isEmpty()) {
      throw new UpdateException("Active entity is not modified");
    }

    return updated.iterator().next();
  }

  @Override
  public final Collection<Entity> update(Collection<? extends Entity> entities) throws DatabaseException, ValidationException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return emptyList();
    }
    states.verifyUpdateEnabled();
    if (entities.size() > 1 && !states.updateMultipleEnabled.get()) {
      throw new IllegalStateException("Batch update of entities is not enabled");
    }

    notifyBeforeUpdate(mapToOriginalPrimaryKey(entities, entities));
    validate(entities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - update {}", this, entities);

    List<Entity> updatedEntities = new ArrayList<>(update(new ArrayList<>(entities), connectionProvider.connection()));
    int index = updatedEntities.indexOf(entity);
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    notifyAfterUpdate(mapToOriginalPrimaryKey(entities, updatedEntities));

    return updatedEntities;
  }

  @Override
  public final void delete() throws DatabaseException {
    Entity originalEntity = entity.copy();
    originalEntity.revert();

    delete(singletonList(originalEntity));
  }

  @Override
  public final void delete(Collection<? extends Entity> entities) throws DatabaseException {
    if (requireNonNull(entities, ENTITIES).isEmpty()) {
      return;
    }
    states.verifyDeleteEnabled();
    LOG.debug("{} - delete {}", this, entities);

    notifyBeforeDelete(unmodifiableCollection(entities));

    delete(entities, connectionProvider.connection());
    if (entities.contains(entity)) {
      setDefaults();
    }

    notifyAfterDelete(unmodifiableCollection(entities));
  }

  @Override
  public final void refreshEntity() {
    try {
      if (states.entityExists.get()) {
        set(connectionProvider().connection().select(entity.primaryKey()));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean containsSearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeys().definition(foreignKey);
    synchronized (entitySearchModels) {
      return entitySearchModels.containsKey(foreignKey);
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
    return (Value<T>) editModelValues.computeIfAbsent(attribute, k -> new EditModelValue<>(this, attribute));
  }

  @Override
  public final <T> void removeEditListener(Attribute<T> attribute, Consumer<T> listener) {
    events.removeEditListener(attribute, listener);
  }

  @Override
  public final <T> void addEditListener(Attribute<T> attribute, Consumer<T> listener) {
    events.addEditListener(attribute, listener);
  }

  @Override
  public final <T> void removeValueListener(Attribute<T> attribute, Consumer<T> listener) {
    events.removeValueListener(attribute, listener);
  }

  @Override
  public final <T> void addValueListener(Attribute<T> attribute, Consumer<T> listener) {
    events.addValueListener(attribute, listener);
  }

  @Override
  public final void removeValueChangeListener(Consumer<Attribute<?>> listener) {
    events.valueChange.removeDataListener(listener);
  }

  @Override
  public final void addValueChangeListener(Consumer<Attribute<?>> listener) {
    events.valueChange.addDataListener(listener);
  }

  @Override
  public final void removeEntityListener(Consumer<Entity> listener) {
    events.entity.removeDataListener(listener);
  }

  @Override
  public final void addEntityListener(Consumer<Entity> listener) {
    events.entity.addDataListener(listener);
  }

  @Override
  public final void removeBeforeInsertListener(Consumer<Collection<Entity>> listener) {
    events.beforeInsert.removeDataListener(listener);
  }

  @Override
  public final void addBeforeInsertListener(Consumer<Collection<Entity>> listener) {
    events.beforeInsert.addDataListener(listener);
  }

  @Override
  public final void removeAfterInsertListener(Consumer<Collection<Entity>> listener) {
    events.afterInsert.removeDataListener(listener);
  }

  @Override
  public final void addAfterInsertListener(Consumer<Collection<Entity>> listener) {
    events.afterInsert.addDataListener(listener);
  }

  @Override
  public final void removeBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener) {
    events.beforeUpdate.removeDataListener(listener);
  }

  @Override
  public final void addBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener) {
    events.beforeUpdate.addDataListener(listener);
  }

  @Override
  public final void removeAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener) {
    events.afterUpdate.removeDataListener(listener);
  }

  @Override
  public final void addAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener) {
    events.afterUpdate.addDataListener(listener);
  }

  @Override
  public final void addBeforeDeleteListener(Consumer<Collection<Entity>> listener) {
    events.beforeDelete.addDataListener(listener);
  }

  @Override
  public final void removeBeforeDeleteListener(Consumer<Collection<Entity>> listener) {
    events.beforeDelete.removeDataListener(listener);
  }

  @Override
  public final void removeAfterDeleteListener(Consumer<Collection<Entity>> listener) {
    events.afterDelete.removeDataListener(listener);
  }

  @Override
  public final void addAfterDeleteListener(Consumer<Collection<Entity>> listener) {
    events.afterDelete.addDataListener(listener);
  }

  @Override
  public final void removeInsertUpdateOrDeleteListener(Runnable listener) {
    events.insertUpdateOrDelete.removeListener(listener);
  }

  @Override
  public final void addInsertUpdateOrDeleteListener(Runnable listener) {
    events.insertUpdateOrDelete.addListener(listener);
  }

  @Override
  public final void addConfirmOverwriteListener(Consumer<State> listener) {
    events.confirmOverwrite.addDataListener(listener);
  }

  @Override
  public final void removeConfirmOverwriteListener(Consumer<State> listener) {
    events.confirmOverwrite.removeDataListener(listener);
  }

  /**
   * Inserts the given entities into the database using the given connection
   * @param entities the entities to insert
   * @param connection the connection to use
   * @return the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  protected Collection<Entity> insert(Collection<? extends Entity> entities, EntityConnection connection) throws DatabaseException {
    return requireNonNull(connection).insertSelect(entities);
  }

  /**
   * Updates the given entities in the database using the given connection
   * @param entities the entities to update
   * @param connection the connection to use
   * @return the updated entities
   * @throws DatabaseException in case of a database exception
   */
  protected Collection<Entity> update(Collection<? extends Entity> entities, EntityConnection connection) throws DatabaseException {
    return requireNonNull(connection).updateSelect(entities);
  }

  /**
   * Deletes the given entities from the database using the given connection
   * @param entities the entities to delete
   * @param connection the connection to use
   * @throws DatabaseException in case of a database exception
   */
  protected void delete(Collection<? extends Entity> entities, EntityConnection connection) throws DatabaseException {
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
    Entity currentForeignKeyValue = referencedEntity(foreignKey);
    if (currentForeignKeyValue != null) {
      for (Entity replacementValue : values) {
        if (currentForeignKeyValue.equals(replacementValue)) {
          put(foreignKey, null);
          put(foreignKey, replacementValue);
        }
      }
    }
  }

  /**
   * Controls the validator used by this edit model.
   * @return the value controlling the validator
   * @see #validate(Entity)
   */
  protected final Value<EntityValidator> validator() {
    return validator;
  }

  /**
   * Controls the 'modified' predicate for this edit model, which is responsible for providing
   * the modified state of the underlying entity.
   * @return the value controlling the predicate used to check if the entity is modified
   * @see Entity#modified()
   * @see #modified()
   */
  protected final Value<Predicate<Entity>> modifiedPredicate() {
    return modifiedPredicate;
  }

  /**
   * Controls the 'exists' predicate for this edit model, which is responsible for providing
   * the exists state of the underlying entity.
   * @return the value controlling the predicate used to check if the entity exists
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
   * @see #addBeforeInsertListener(Consumer)
   */
  protected final void notifyBeforeInsert(Collection<Entity> entitiesToInsert) {
    events.beforeInsert.accept(requireNonNull(entitiesToInsert));
  }

  /**
   * Notifies that insert has been performed
   * @param insertedEntities the inserted entities
   * @see #addAfterInsertListener(Consumer)
   */
  protected final void notifyAfterInsert(Collection<Entity> insertedEntities) {
    events.afterInsert.accept(requireNonNull(insertedEntities));
  }

  /**
   * Notifies that update is about to be performed
   * @param entitiesToUpdate the entities about to be updated
   * @see #addBeforeUpdateListener(Consumer)
   */
  protected final void notifyBeforeUpdate(Map<Entity.Key, Entity> entitiesToUpdate) {
    events.beforeUpdate.accept(requireNonNull(entitiesToUpdate));
  }

  /**
   * Notifies that update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(Consumer)
   */
  protected final void notifyAfterUpdate(Map<Entity.Key, Entity> updatedEntities) {
    events.afterUpdate.accept(requireNonNull(updatedEntities));
  }

  /**
   * Notifies that delete is about to be performed
   * @param entitiesToDelete the entities about to be deleted
   * @see #addBeforeDeleteListener(Consumer)
   */
  protected final void notifyBeforeDelete(Collection<Entity> entitiesToDelete) {
    events.beforeDelete.accept(requireNonNull(entitiesToDelete));
  }

  /**
   * Notifies that delete has been performed
   * @param deletedEntities the deleted entities
   * @see #addAfterDeleteListener(Consumer)
   */
  protected final void notifyAfterDelete(Collection<Entity> deletedEntities) {
    events.afterDelete.accept(requireNonNull(deletedEntities));
  }

  private Collection<Entity> insertEntities(Collection<? extends Entity> entities) throws DatabaseException, ValidationException {
    notifyBeforeInsert(unmodifiableCollection(entities));
    validate(entities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - insert {}", this, entities);

    return insert(entities, connectionProvider.connection());
  }

  private boolean setEntityAllowed() {
    if (states.overwriteWarning.get() && exists().get() && modified().get()) {
      State confirmation = State.state(true);
      events.confirmOverwrite.accept(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void setEntity(Entity entity) {
    Map<Attribute<?>, Object> affectedAttributes = this.entity.set(entity == null ? defaultEntity(this::defaultValue) : entity);
    for (Attribute<?> affectedAttribute : affectedAttributes.keySet()) {
      Attribute<Object> objectAttribute = (Attribute<Object>) affectedAttribute;
      events.notifyValueChange(objectAttribute, this.entity.get(objectAttribute));
    }
    if (affectedAttributes.isEmpty()) {//otherwise notifyValueChange() triggers entity state updates
      states.updateEntityStates();
    }
    states.updateAttributeModifiedStates();

    events.entity.accept(entity);
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
  private Entity defaultEntity(ValueSupplier valueSupplier) {
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
        return (T) entity.referencedEntity((ForeignKey) attributeDefinition.attribute());
      }

      return entity.get(attributeDefinition.attribute());
    }

    return (T) defaultValueSuppliers.computeIfAbsent(attributeDefinition.attribute(), k -> attributeDefinition::defaultValue).get();
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
      dependingValues.put(derivedAttribute, get(derivedAttribute));
      addDependingDerivedAttributes(derivedAttribute, dependingValues);
    });
  }

  private void addDependingForeignKeys(Column<?> column, Map<Attribute<?>, Object> dependingValues) {
    entityDefinition().foreignKeys().definitions(column).forEach(foreignKeyDefinition ->
            dependingValues.put(foreignKeyDefinition.attribute(), get(foreignKeyDefinition.attribute())));
  }

  private void addDependingReferencedColumns(ForeignKey foreignKey, Map<Attribute<?>, Object> dependingValues) {
    foreignKey.references().forEach(reference ->
            dependingValues.put(reference.column(), get(reference.column())));
  }

  private static void addColumnValues(ValueSupplier valueSupplier, EntityDefinition definition, Entity newEntity) {
    definition.columns().definitions().stream()
            .filter(columnDefinition -> !definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute()))//these are set via their respective parent foreign key
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
  private static Map<Entity.Key, Entity> mapToOriginalPrimaryKey(Collection<? extends Entity> entitiesBeforeUpdate,
                                                                 Collection<? extends Entity> entitiesAfterUpdate) {
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

  private final class NotifyInserted implements Consumer<Collection<Entity>> {

    @Override
    public void accept(Collection<Entity> insertedEntities) {
      if (states.editEvents.get()) {
        EntityEditEvents.notifyInserted(insertedEntities);
      }
    }
  }

  private final class NotifyUpdated implements Consumer<Map<Entity.Key, Entity>> {

    @Override
    public void accept(Map<Entity.Key, Entity> updatedEntities) {
      if (states.editEvents.get()) {
        EntityEditEvents.notifyUpdated(updatedEntities);
      }
    }
  }

  private final class NotifyDeleted implements Consumer<Collection<Entity>> {

    @Override
    public void accept(Collection<Entity> deletedEntities) {
      if (states.editEvents.get()) {
        EntityEditEvents.notifyDeleted(deletedEntities);
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
    private final Event<?> insertUpdateOrDelete = Event.event();
    private final Event<State> confirmOverwrite = Event.event();
    private final Event<Entity> entity = Event.event();
    private final Event<Attribute<?>> valueChange = Event.event();
    private final Map<Attribute<?>, Event<?>> valueEditEvents = new ConcurrentHashMap<>();
    private final Map<Attribute<?>, Event<?>> valueChangeEvents = new ConcurrentHashMap<>();

    private void bindEvents() {
      afterInsert.addListener(insertUpdateOrDelete);
      afterUpdate.addListener(insertUpdateOrDelete);
      afterDelete.addListener(insertUpdateOrDelete);
      afterInsert.addDataListener(new NotifyInserted());
      afterUpdate.addDataListener(new NotifyUpdated());
      afterDelete.addDataListener(new NotifyDeleted());
      validator.addListener(states::updateValidState);
      modifiedPredicate.addListener(states::updateModifiedState);
      existsPredicate.addListener(states::updateExistsState);
    }

    private <T> void addEditListener(Attribute<T> attribute, Consumer<T> listener) {
      entityDefinition().attributes().definition(attribute);
      ((Event<T>) valueEditEvents.computeIfAbsent(attribute, k -> Event.event())).addDataListener(listener);
    }

    private <T> void removeEditListener(Attribute<T> attribute, Consumer<T> listener) {
      entityDefinition().attributes().definition(attribute);
      if (valueEditEvents.containsKey(attribute)) {
        ((Event<T>) valueEditEvents.get(attribute)).removeDataListener(listener);
      }
    }

    private <T> void addValueListener(Attribute<T> attribute, Consumer<T> listener) {
      entityDefinition().attributes().definition(attribute);
      ((Event<T>) valueChangeEvents.computeIfAbsent(attribute, k -> Event.event())).addDataListener(listener);
    }

    private <T> void removeValueListener(Attribute<T> attribute, Consumer<T> listener) {
      entityDefinition().attributes().definition(attribute);
      if (valueChangeEvents.containsKey(attribute)) {
        ((Event<T>) valueChangeEvents.get(attribute)).removeDataListener(listener);
      }
    }

    private <T> void notifyValueEdit(Attribute<T> attribute, T value, Map<Attribute<?>, Object> dependingValues) {
      notifyValueChange(attribute, value);
      Event<T> editEvent = (Event<T>) valueEditEvents.get(attribute);
      if (editEvent != null) {
        editEvent.accept(value);
      }
      dependingValues.forEach((dependingAttribute, previousValue) -> {
        Object currentValue = get(dependingAttribute);
        if (!Objects.equals(previousValue, currentValue)) {
          notifyValueEdit((Attribute<Object>) dependingAttribute, currentValue, emptyMap());
        }
      });
    }

    private <T> void notifyValueChange(Attribute<T> attribute, T value) {
      states.updateEntityStates();
      states.updateAttributeStates(attribute);
      Event<T> changeEvent = (Event<T>) valueChangeEvents.get(attribute);
      if (changeEvent != null) {
        changeEvent.accept(value);
      }
      valueChange.accept(attribute);
    }
  }

  private final class States {

    private final State entityValid = State.state();
    private final State entityExists = State.state(false);
    private final State entityModified = State.state();
    private final State primaryKeyNull = State.state(true);
    private final State readOnly = State.state();
    private final State insertEnabled = State.state(true);
    private final State updateEnabled = State.state(true);
    private final State updateMultipleEnabled = State.state(true);
    private final State deleteEnabled = State.state(true);
    private final State overwriteWarning = State.state(WARN_ABOUT_UNSAVED_DATA.get());
    private final State editEvents = State.state(EDIT_EVENTS.get());
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

    private void verifyUpdateEnabled() {
      if (readOnly.get() || !updateEnabled.get()) {
        throw new IllegalStateException("Edit model is readOnly or updating is not enabled!");
      }
    }

    private void verifyDeleteEnabled() {
      if (readOnly.get() || !deleteEnabled.get()) {
        throw new IllegalStateException("Edit model is readOnly or deleting is not enabled!");
      }
    }
  }

  private static final class EditModelValue<T> extends AbstractValue<T> {

    private final EntityEditModel editModel;
    private final Attribute<T> attribute;

    private EditModelValue(EntityEditModel editModel, Attribute<T> attribute) {
      this.editModel = editModel;
      this.attribute = attribute;
      this.editModel.addValueListener(attribute, valueChange -> notifyListeners());
    }

    @Override
    public T get() {
      return editModel.get(attribute);
    }

    @Override
    protected void setValue(T value) {
      editModel.put(attribute, value);
    }
  }
}