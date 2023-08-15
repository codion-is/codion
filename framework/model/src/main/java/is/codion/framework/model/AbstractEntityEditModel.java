/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.Key;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class AbstractEntityEditModel implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

  private static final String ENTITIES = "entities";

  private final Event<Collection<Entity>> beforeInsertEvent = Event.event();
  private final Event<Collection<Entity>> afterInsertEvent = Event.event();
  private final Event<Map<Key, Entity>> beforeUpdateEvent = Event.event();
  private final Event<Map<Key, Entity>> afterUpdateEvent = Event.event();
  private final Event<Collection<Entity>> beforeDeleteEvent = Event.event();
  private final Event<Collection<Entity>> afterDeleteEvent = Event.event();
  private final Event<?> entitiesEditedEvent = Event.event();
  private final Event<State> confirmSetEntityEvent = Event.event();
  private final Event<Entity> entityEvent = Event.event();
  private final Event<Attribute<?>> valueChangeEvent = Event.event();
  private final Event<?> refreshEvent = Event.event();

  private final State entityValidState = State.state();
  private final State entityNewState = State.state(true);
  private final State entityModifiedState = State.state();
  private final State primaryKeyNullState = State.state(true);
  private final State insertEnabledState = State.state(true);
  private final State updateEnabledState = State.state(true);
  private final State deleteEnabledState = State.state(true);
  private final StateObserver readOnlyObserver = State.and(insertEnabledState.reversed(),
          updateEnabledState.reversed(), deleteEnabledState.reversed());
  private final Map<Attribute<?>, State> attributeModifiedStateMap = new HashMap<>();
  private final Map<Attribute<?>, State> attributeNullStateMap = new HashMap<>();

  /**
   * The Entity being edited by this model
   */
  private final Entity entity;

  /**
   * The {@link EntityConnectionProvider} instance to use
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the {@link EntitySearchModel}s used by this {@link EntityEditModel}
   */
  private final Map<ForeignKey, EntitySearchModel> entitySearchModels = new HashMap<>();

  /**
   * Holds the edit model values created via {@link #value(Attribute)}
   */
  private final Map<Attribute<?>, Value<?>> editModelValues = new ConcurrentHashMap<>();

  /**
   * Contains true if values should persist for the given attribute when the model is cleared
   */
  private final Map<Attribute<?>, Boolean> persistentValues = new HashMap<>();

  /**
   * The validator used by this edit model
   */
  private final EntityValidator validator;

  /**
   * Holds events signaling value changes made via {@link #put(Attribute, Object)} or {@link #remove(Attribute)}
   */
  private final Map<Attribute<?>, Event<?>> valueEditEvents = new ConcurrentHashMap<>();

  /**
   * Holds events signaling value changes in the underlying {@link Entity}
   */
  private final Map<Attribute<?>, Event<?>> valueChangeEvents = new ConcurrentHashMap<>();

  /**
   * Holds the default value suppliers for attributes
   */
  private final Map<Attribute<?>, Supplier<?>> defaultValueSuppliers = new ConcurrentHashMap<>();

  /**
   * Provides this model with a way to check if the underlying entity is in a modified state.
   */
  private Supplier<Boolean> modifiedSupplier;

  /**
   * Specifies whether this edit model should warn about unsaved data
   */
  private boolean warnAboutUnsavedData = WARN_ABOUT_UNSAVED_DATA.get();

  /**
   * Specifies whether this edit model posts insert, update and delete events
   * on the {@link EntityEditEvents} event bus.
   */
  private boolean postEditEvents = POST_EDIT_EVENTS.get();

  /**
   * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(requireNonNull(entityType), requireNonNull(connectionProvider), connectionProvider.entities().definition(entityType).validator());
  }

  /**
   * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                    EntityValidator validator) {
    this.entity = requireNonNull(connectionProvider).entities().entity(entityType);
    this.connectionProvider = connectionProvider;
    this.validator = requireNonNull(validator);
    this.modifiedSupplier = entity::isModified;
    setReadOnly(entityDefinition().isReadOnly());
    configurePersistentForeignKeyValues();
    bindEventsInternal();
    doSetEntity(defaultEntity(AttributeDefinition::defaultValue));
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
    return getClass() + ", " + entity.type();
  }

  @Override
  public final <T> void setDefaultValueSupplier(Attribute<T> attribute, Supplier<T> valueSupplier) {
    entityDefinition().attributeDefinition(attribute);
    defaultValueSuppliers.put(attribute, requireNonNull(valueSupplier, "valueSupplier"));
  }

  @Override
  public final void setModifiedSupplier(Supplier<Boolean> modifiedSupplier) {
    this.modifiedSupplier = requireNonNull(modifiedSupplier, "modifiedSupplier");
  }

  @Override
  public final boolean isReadOnly() {
    return readOnlyObserver.get();
  }

  @Override
  public final void setReadOnly(boolean readOnly) {
    insertEnabledState.set(!readOnly);
    updateEnabledState.set(!readOnly);
    deleteEnabledState.set(!readOnly);
  }

  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  @Override
  public final void setWarnAboutUnsavedData(boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
  }

  @Override
  public final boolean isPersistValue(Attribute<?> attribute) {
    entityDefinition().attributeDefinition(attribute);

    return Boolean.TRUE.equals(persistentValues.get(attribute));
  }

  @Override
  public final void setPersistValue(Attribute<?> attribute, boolean persistValue) {
    entityDefinition().attributeDefinition(attribute);
    persistentValues.put(attribute, persistValue);
  }

  @Override
  public final boolean isInsertEnabled() {
    return insertEnabledState.get();
  }

  @Override
  public final void setInsertEnabled(boolean insertEnabled) {
    insertEnabledState.set(insertEnabled);
  }

  @Override
  public final StateObserver insertEnabledObserver() {
    return insertEnabledState.observer();
  }

  @Override
  public final boolean isUpdateEnabled() {
    return updateEnabledState.get();
  }

  @Override
  public final void setUpdateEnabled(boolean updateEnabled) {
    updateEnabledState.set(updateEnabled);
  }

  @Override
  public final StateObserver updateEnabledObserver() {
    return updateEnabledState.observer();
  }

  @Override
  public final boolean isDeleteEnabled() {
    return deleteEnabledState.get();
  }

  @Override
  public final void setDeleteEnabled(boolean deleteEnabled) {
    deleteEnabledState.set(deleteEnabled);
  }

  @Override
  public final StateObserver deleteEnabledObserver() {
    return deleteEnabledState.observer();
  }

  @Override
  public final StateObserver entityNewObserver() {
    return entityNewState.observer();
  }

  @Override
  public final StateObserver primaryKeyNullObserver() {
    return primaryKeyNullState.observer();
  }

  @Override
  public final void setEntity(Entity entity) {
    if (isSetEntityAllowed()) {
      doSetEntity(entity);
    }
  }

  @Override
  public final void setDefaultValues() {
    if (isSetEntityAllowed()) {
      doSetEntity(null);
    }
  }

  @Override
  public final EntityType entityType() {
    return entity.type();
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void replaceForeignKeyValues(ForeignKey foreignKey, Collection<Entity> entities) {
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
  public StateObserver modifiedObserver() {
    return entityModifiedState.observer();
  }

  @Override
  public final boolean isModified() {
    return modifiedSupplier.get();
  }

  @Override
  public final StateObserver modifiedObserver(Attribute<?> attribute) {
    entityDefinition().attributeDefinition(attribute);

    return attributeModifiedStateMap.computeIfAbsent(attribute, k ->
            State.state(!entity.isNew() && entity.isModified(attribute))).observer();
  }

  @Override
  public final StateObserver nullObserver(Attribute<?> attribute) {
    entityDefinition().attributeDefinition(attribute);

    return attributeNullStateMap.computeIfAbsent(attribute, k ->
            State.state(entity.isNull(attribute))).observer();
  }

  @Override
  public boolean isEntityNew() {
    return entity.isNew();
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
    entityDefinition().attributeDefinition(attribute);
    Map<Attribute<?>, Object> dependingValues = dependendingValues(attribute);
    T previousValue = entity.put(attribute, value);
    if (!Objects.equals(value, previousValue)) {
      notifyValueEdit(attribute, value, dependingValues);
    }

    return previousValue;
  }

  @Override
  public final <T> T remove(Attribute<T> attribute) {
    entityDefinition().attributeDefinition(attribute);
    T value = null;
    if (entity.contains(attribute)) {
      Map<Attribute<?>, Object> dependingValues = dependendingValues(attribute);
      value = entity.remove(attribute);
      notifyValueEdit(attribute, null, dependingValues);
    }

    return value;
  }

  @Override
  public final boolean isNullable(Attribute<?> attribute) {
    return validator.isNullable(entity, attribute);
  }

  @Override
  public final boolean isNull(Attribute<?> attribute) {
    return entity.isNull(attribute);
  }

  @Override
  public final boolean isNotNull(Attribute<?> attribute) {
    return !entity.isNull(attribute);
  }

  @Override
  public final StateObserver validObserver() {
    return entityValidState.observer();
  }

  @Override
  public final boolean isValid() {
    return entityValidState.get();
  }

  @Override
  public final void validate(Attribute<?> attribute) throws ValidationException {
    validator.validate(entity, attribute);
  }

  @Override
  public final void validate() throws ValidationException {
    validate(entity);
  }

  @Override
  public final void validate(Collection<? extends Entity> entities) throws ValidationException {
    for (Entity entityToValidate : entities) {
      validate(entityToValidate);
    }
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    if (entity.type().equals(entityType())) {
      validator.validate(entity);
    }
    else {
      entity.definition().validator().validate(entity);
    }
  }

  @Override
  public final boolean isValid(Attribute<?> attribute) {
    try {
      validate(attribute);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  @Override
  public final EntityValidator validator() {
    return validator;
  }

  @Override
  public final Entity insert() throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    Entity toInsert = entity.copy();
    if (entityDefinition().isKeyGenerated()) {
      toInsert.clearPrimaryKey();
    }
    toInsert.saveAll();
    Collection<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    Entity inserted = insertedEntities.iterator().next();
    doSetEntity(inserted);

    notifyAfterInsert(unmodifiableCollection(insertedEntities));

    return inserted;
  }

  @Override
  public final Collection<Entity> insert(Collection<? extends Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
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
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }

    Collection<Entity> modifiedEntities = modified(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    notifyBeforeUpdate(mapToOriginalPrimaryKey(modifiedEntities, entities));
    validate(modifiedEntities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - update {}", this, entities);

    List<Entity> updatedEntities = new ArrayList<>(doUpdate(new ArrayList<>(modifiedEntities)));
    int index = updatedEntities.indexOf(entity);
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    notifyAfterUpdate(mapToOriginalPrimaryKey(modifiedEntities, updatedEntities));

    return updatedEntities;
  }

  @Override
  public final void delete() throws DatabaseException {
    Entity originalEntity = entity.copy();
    originalEntity.revertAll();

    delete(singletonList(originalEntity));
  }

  @Override
  public final void delete(Collection<? extends Entity> entities) throws DatabaseException {
    requireNonNull(entities, ENTITIES);
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Delete is not enabled!");
    }
    if (entities.isEmpty()) {
      return;
    }
    LOG.debug("{} - delete {}", this, entities);

    notifyBeforeDelete(unmodifiableCollection(entities));

    doDelete(entities);
    if (entities.contains(entity)) {
      doSetEntity(null);
    }

    notifyAfterDelete(unmodifiableCollection(entities));
  }

  @Override
  public final void refresh() {
    refreshDataModels();
    refreshEvent.run();
  }

  @Override
  public final void refreshEntity() {
    try {
      if (!isEntityNew()) {
        setEntity(connectionProvider().connection().select(entity.primaryKey()));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean containsSearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyDefinition(foreignKey);
    synchronized (entitySearchModels) {
      return entitySearchModels.containsKey(foreignKey);
    }
  }

  @Override
  public EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition().foreignKeyDefinition(foreignKey);
    Collection<Column<String>> searchColumns = entities()
            .definition(foreignKeyDefinition.referencedType()).searchColumns();
    if (searchColumns.isEmpty()) {
      throw new IllegalStateException("No search columns defined for entity: " + foreignKeyDefinition.referencedType());
    }

    return EntitySearchModel.builder(foreignKeyDefinition.referencedType(), connectionProvider)
            .searchColumns(searchColumns)
            .singleSelection(true)
            .build();
  }

  @Override
  public final EntitySearchModel foreignKeySearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyDefinition(foreignKey);
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
    entityDefinition().attributeDefinition(attribute);
    return (Value<T>) editModelValues.computeIfAbsent(attribute, k -> new EditModelValue<>(this, attribute));
  }

  @Override
  public final boolean containsUnsavedData() {
    return !isEntityNew() && modifiedObserver().get();
  }

  @Override
  public final boolean isPostEditEvents() {
    return postEditEvents;
  }

  @Override
  public final void setPostEditEvents(boolean postEditEvents) {
    this.postEditEvents = postEditEvents;
  }

  @Override
  public final <T> void removeEditListener(Attribute<T> attribute, Consumer<T> listener) {
    if (valueEditEvents.containsKey(attribute)) {
      ((Event<T>) valueEditEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addEditListener(Attribute<T> attribute, Consumer<T> listener) {
    editEvent(attribute).addDataListener(listener);
  }

  @Override
  public final <T> void removeValueListener(Attribute<T> attribute, Consumer<T> listener) {
    if (valueChangeEvents.containsKey(attribute)) {
      ((Event<T>) valueChangeEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addValueListener(Attribute<T> attribute, Consumer<T> listener) {
    valueEvent(attribute).addDataListener(listener);
  }

  @Override
  public final void removeValueListener(Consumer<Attribute<?>> listener) {
    valueChangeEvent.removeDataListener(listener);
  }

  @Override
  public final void addValueListener(Consumer<Attribute<?>> listener) {
    valueChangeEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntityListener(Consumer<Entity> listener) {
    entityEvent.removeDataListener(listener);
  }

  @Override
  public final void addEntityListener(Consumer<Entity> listener) {
    entityEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeInsertListener(Consumer<Collection<Entity>> listener) {
    beforeInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeInsertListener(Consumer<Collection<Entity>> listener) {
    beforeInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterInsertListener(Consumer<Collection<Entity>> listener) {
    afterInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterInsertListener(Consumer<Collection<Entity>> listener) {
    afterInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeUpdateListener(Consumer<Map<Key, Entity>> listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeUpdateListener(Consumer<Map<Key, Entity>> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterUpdateListener(Consumer<Map<Key, Entity>> listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterUpdateListener(Consumer<Map<Key, Entity>> listener) {
    afterUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void addBeforeDeleteListener(Consumer<Collection<Entity>> listener) {
    beforeDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeDeleteListener(Consumer<Collection<Entity>> listener) {
    beforeDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void removeAfterDeleteListener(Consumer<Collection<Entity>> listener) {
    afterDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterDeleteListener(Consumer<Collection<Entity>> listener) {
    afterDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntitiesEditedListener(Runnable listener) {
    entitiesEditedEvent.removeListener(listener);
  }

  @Override
  public final void addEntitiesEditedListener(Runnable listener) {
    entitiesEditedEvent.addListener(listener);
  }

  @Override
  public final void addConfirmSetEntityObserver(Consumer<State> listener) {
    confirmSetEntityEvent.addDataListener(listener);
  }

  @Override
  public final void removeConfirmSetEntityObserver(Consumer<State> listener) {
    confirmSetEntityEvent.removeDataListener(listener);
  }

  @Override
  public final void addRefreshListener(Runnable listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(Runnable listener) {
    refreshEvent.removeListener(listener);
  }

  /**
   * Inserts the given entities into the database
   * @param entities the entities to insert
   * @return the primary keys of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  protected Collection<Key> doInsert(Collection<? extends Entity> entities) throws DatabaseException {
    return connectionProvider.connection().insert(entities);
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return the updated entities
   * @throws DatabaseException in case of a database exception
   */
  protected Collection<Entity> doUpdate(Collection<? extends Entity> entities) throws DatabaseException {
    return connectionProvider.connection().update(entities);
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws DatabaseException in case of a database exception
   */
  protected void doDelete(Collection<? extends Entity> entities) throws DatabaseException {
    connectionProvider.connection().delete(Entity.primaryKeys(entities));
  }

  /**
   * Called during the {@link #update()} function, to determine which entities need to be updated,
   * these entities will then be forwarded to {@link #doUpdate(Collection)}.
   * Returns the entities that have been modified and require updating, override to be able to
   * perform an update on unmodified entities or to return an empty list to veto an update action.
   * @param entities the entities
   * @return the entities requiring update
   * @see #update()
   * @see #update(Collection)
   */
  protected Collection<Entity> modified(Collection<? extends Entity> entities) {
    return Entity.modified(entities);
  }

  /**
   * Refresh all data-models used by this edit model, combo box models and such.
   */
  protected void refreshDataModels() {}

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
   * @return the State used to indicate the modified state of this edit model, handle with care
   */
  protected final State modifiedState() {
    return entityModifiedState;
  }

  /**
   * Notifies that insert is about to be performed
   * @param entitiesToInsert the entities about to be inserted
   * @see #addBeforeInsertListener(Consumer)
   */
  protected final void notifyBeforeInsert(Collection<Entity> entitiesToInsert) {
    beforeInsertEvent.accept(entitiesToInsert);
  }

  /**
   * Notifies that insert has been performed
   * @param insertedEntities the inserted entities
   * @see #addAfterInsertListener(Consumer)
   */
  protected final void notifyAfterInsert(Collection<Entity> insertedEntities) {
    afterInsertEvent.accept(insertedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyInserted(insertedEntities);
    }
  }

  /**
   * Notifies that update is about to be performed
   * @param entitiesToUpdate the entities about to be updated
   * @see #addBeforeUpdateListener(Consumer)
   */
  protected final void notifyBeforeUpdate(Map<Key, Entity> entitiesToUpdate) {
    beforeUpdateEvent.accept(entitiesToUpdate);
  }

  /**
   * Notifies that update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(Consumer)
   */
  protected final void notifyAfterUpdate(Map<Key, Entity> updatedEntities) {
    afterUpdateEvent.accept(updatedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyUpdated(updatedEntities);
    }
  }

  /**
   * Notifies that delete is about to be performed
   * @param entitiesToDelete the entities about to be deleted
   * @see #addBeforeDeleteListener(Consumer)
   */
  protected final void notifyBeforeDelete(Collection<Entity> entitiesToDelete) {
    beforeDeleteEvent.accept(entitiesToDelete);
  }

  /**
   * Notifies that delete has been performed
   * @param deletedEntities the deleted entities
   * @see #addAfterDeleteListener(Consumer)
   */
  protected final void notifyAfterDelete(Collection<Entity> deletedEntities) {
    afterDeleteEvent.accept(deletedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyDeleted(deletedEntities);
    }
  }

  private Collection<Entity> insertEntities(Collection<? extends Entity> entities) throws DatabaseException, ValidationException {
    notifyBeforeInsert(unmodifiableCollection(entities));
    validate(entities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - insert {}", this, entities);

    return connectionProvider.connection().select(doInsert(entities));
  }

  private boolean isSetEntityAllowed() {
    if (warnAboutUnsavedData && containsUnsavedData()) {
      State confirmation = State.state(true);
      confirmSetEntityEvent.accept(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void doSetEntity(Entity entity) {
    Map<Attribute<?>, Object> affectedAttributes = this.entity.set(entity == null ? defaultEntity(this::defaultValue) : entity);
    for (Map.Entry<Attribute<?>, Object> entry : affectedAttributes.entrySet()) {
      Attribute<Object> objectAttribute = (Attribute<Object>) entry.getKey();
      onValueChange(objectAttribute, this.entity.get(objectAttribute));
    }
    if (affectedAttributes.isEmpty()) {//no value changes to trigger state updates
      updateEntityStates();
    }
    updateModifiedAttributeStates();

    entityEvent.accept(entity);
  }

  private <T> Event<T> editEvent(Attribute<T> attribute) {
    entityDefinition().attributeDefinition(attribute);
    return (Event<T>) valueEditEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private <T> Event<T> valueEvent(Attribute<T> attribute) {
    entityDefinition().attributeDefinition(attribute);
    return (Event<T>) valueChangeEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private void configurePersistentForeignKeyValues() {
    if (EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get()) {
      entityDefinition().foreignKeys().forEach(foreignKey -> setPersistValue(foreignKey, isForeignKeyWritable(foreignKey)));
    }
  }

  private boolean isForeignKeyWritable(ForeignKey foreignKey) {
    return foreignKey.references().stream()
            .map(ForeignKey.Reference::column)
            .map(entityDefinition()::columnDefinition)
            .filter(ColumnDefinition.class::isInstance)
            .map(ColumnDefinition.class::cast)
            .anyMatch(columnDefinition -> !columnDefinition.isReadOnly());
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
    definition.columnDefinitions().stream()
            .filter(columnDefinition -> !definition.isForeignKeyColumn(columnDefinition.attribute()))//these are set via their respective parent foreign key
            .filter(columnDefinition -> !columnDefinition.columnHasDefaultValue() || columnDefinition.hasDefaultValue())
            .map(columnDefinition -> (AttributeDefinition<Object>) columnDefinition)
            .forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
    definition.transientAttributeDefinitions().stream()
            .filter(attributeDefinition -> !attributeDefinition.isDerived())
            .map(attributeDefinition -> (AttributeDefinition<Object>) attributeDefinition)
            .forEach(attributeDefinition -> newEntity.put(attributeDefinition.attribute(), valueSupplier.get(attributeDefinition)));
    definition.foreignKeyDefinitions().forEach(foreignKeyDefinition ->
            newEntity.put(foreignKeyDefinition.attribute(), valueSupplier.get(foreignKeyDefinition)));

    newEntity.saveAll();

    return newEntity;
  }

  private <T> T defaultValue(AttributeDefinition<T> attributeDefinition) {
    if (isPersistValue(attributeDefinition.attribute())) {
      if (attributeDefinition instanceof ForeignKeyDefinition) {
        return (T) entity.referencedEntity((ForeignKey) attributeDefinition.attribute());
      }

      return entity.get(attributeDefinition.attribute());
    }

    return (T) defaultValueSuppliers.computeIfAbsent(attributeDefinition.attribute(), k -> attributeDefinition::defaultValue).get();
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesEditedEvent);
    afterInsertEvent.addListener(entitiesEditedEvent);
    afterUpdateEvent.addListener(entitiesEditedEvent);
  }

  private Map<Attribute<?>, Object> dependendingValues(Attribute<?> attribute) {
    Map<Attribute<?>, Object> dependentValues = new HashMap<>();
    EntityDefinition entityDefinition = entityDefinition();
    entityDefinition.derivedAttributes(attribute).forEach(derivedAttribute ->
            dependentValues.put(derivedAttribute, get(derivedAttribute)));
    if (attribute instanceof Column) {
      entityDefinition.foreignKeyDefinitions((Column<?>) attribute).forEach(foreignKeyDefinition ->
              dependentValues.put(foreignKeyDefinition.attribute(), get(foreignKeyDefinition.attribute())));
    }
    if (attribute instanceof ForeignKey) {
      ((ForeignKey) attribute).references().forEach(reference ->
              dependentValues.put(reference.column(), get(reference.column())));
    }

    return dependentValues;
  }

  private <T> void notifyValueEdit(Attribute<T> attribute, T value, Map<Attribute<?>, Object> dependingValues) {
    onValueChange(attribute, value);
    editEvent(attribute).accept(value);
    dependingValues.forEach((dependingAttribute, previousValue) -> {
      Object currentValue = get(dependingAttribute);
      if (!Objects.equals(previousValue, currentValue)) {
        notifyValueEdit((Attribute<Object>) dependingAttribute, currentValue, dependendingValues(dependingAttribute));
      }
    });
  }

  private <T> void onValueChange(Attribute<T> attribute, T value) {
    updateEntityStates();
    State nullState = attributeNullStateMap.get(attribute);
    if (nullState != null) {
      nullState.set(entity.isNull(attribute));
    }
    State modifiedState = attributeModifiedStateMap.get(attribute);
    if (modifiedState != null) {
      updateModifiedAttributeState(attribute, modifiedState);
    }
    Event<T> changeEvent = (Event<T>) valueChangeEvents.get(attribute);
    if (changeEvent != null) {
      changeEvent.accept(value);
    }
    valueChangeEvent.accept(attribute);
  }

  private void updateModifiedAttributeStates() {
    attributeModifiedStateMap.forEach(this::updateModifiedAttributeState);
  }

  private void updateModifiedAttributeState(Attribute<?> attribute, State modifiedState) {
    modifiedState.set(!entity.isNew() && entity.isModified(attribute));
  }

  private void updateEntityStates() {
    entityModifiedState.set(isModified());
    entityValidState.set(validator.isValid(entity));
    primaryKeyNullState.set(entity.primaryKey().isNull());
    entityNewState.set(isEntityNew());
  }

  /**
   * Maps the given entities and their updated counterparts to their original primary keys,
   * assumes a single copy of each entity in the given lists.
   * @param entitiesBeforeUpdate the entities before update
   * @param entitiesAfterUpdate the entities after update
   * @return the updated entities mapped to their respective original primary keys
   */
  private static Map<Key, Entity> mapToOriginalPrimaryKey(Collection<Entity> entitiesBeforeUpdate,
                                                          Collection<? extends Entity> entitiesAfterUpdate) {
    List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
    Map<Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
    for (Entity entity : entitiesBeforeUpdate) {
      keyMap.put(entity.originalPrimaryKey(), findAndRemove(entity.primaryKey(), entitiesAfterUpdateCopy.listIterator()));
    }

    return unmodifiableMap(keyMap);
  }

  private static Entity findAndRemove(Key primaryKey, ListIterator<Entity> iterator) {
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

  private static final class EditModelValue<T> extends AbstractValue<T> {

    private final EntityEditModel editModel;
    private final Attribute<T> attribute;

    private EditModelValue(EntityEditModel editModel, Attribute<T> attribute) {
      this.editModel = editModel;
      this.attribute = attribute;
      this.editModel.addValueListener(attribute, valueChange -> notifyValueChange());
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