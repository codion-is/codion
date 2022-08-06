/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.UpdateException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

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
import java.util.function.Supplier;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityEditModel} implementation
 */
public abstract class DefaultEntityEditModel implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private static final String ENTITIES = "entities";
  private static final String PROPERTY = "property";

  private final Event<List<Entity>> beforeInsertEvent = Event.event();
  private final Event<List<Entity>> afterInsertEvent = Event.event();
  private final Event<Map<Key, Entity>> beforeUpdateEvent = Event.event();
  private final Event<Map<Key, Entity>> afterUpdateEvent = Event.event();
  private final Event<List<Entity>> beforeDeleteEvent = Event.event();
  private final Event<List<Entity>> afterDeleteEvent = Event.event();
  private final Event<?> entitiesEditedEvent = Event.event();
  private final Event<State> confirmSetEntityEvent = Event.event();
  private final Event<Entity> entitySetEvent = Event.event();
  private final Event<Attribute<?>> valueChangeEvent = Event.event();
  private final Event<?> refreshEvent = Event.event();

  private final State entityValidState = State.state();
  private final State entityNewState = State.state(true);
  private final State entityModifiedState = State.state();
  private final State primaryKeyNullState = State.state(true);
  private final State insertEnabledState = State.state(true);
  private final State updateEnabledState = State.state(true);
  private final State deleteEnabledState = State.state(true);
  private final StateObserver readOnlyObserver = State.and(insertEnabledState.reversedObserver(),
          updateEnabledState.reversedObserver(), deleteEnabledState.reversedObserver());

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
   * Contains true if values should persist for the given property when the model is cleared
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
   * Specifies whether this edit model sets the foreign key to null when initialized with a null value.
   * @see #initialize(ForeignKey, Entity)
   */
  private boolean initializeForeignKeyToNull = INITIALIZE_FOREIGN_KEY_TO_NULL.get();

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public DefaultEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.entities().getDefinition(entityType).validator());
  }

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                EntityValidator validator) {
    this.entity = connectionProvider.entities().entity(entityType);
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.validator = validator;
    this.modifiedSupplier = entity::isModified;
    setReadOnly(entityDefinition().isReadOnly());
    initializePersistentValues();
    bindEventsInternal();
    doSetEntity(getDefaultEntity(Property::defaultValue));
  }

  @Override
  public final Entities entities() {
    return connectionProvider.entities();
  }

  @Override
  public final EntityDefinition entityDefinition() {
    return entity.entityDefinition();
  }

  @Override
  public final String toString() {
    return getClass().toString() + ", " + entity.entityType();
  }

  @Override
  public final <T> void setDefaultValueSupplier(Attribute<T> attribute, Supplier<T> valueSupplier) {
    requireNonNull(valueSupplier, "valueSupplier");
    entityDefinition().property(attribute);
    defaultValueSuppliers.put(attribute, valueSupplier);
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
  public boolean isPersistValue(Attribute<?> attribute) {
    entityDefinition().property(attribute);

    return Boolean.TRUE.equals(persistentValues.get(attribute));
  }

  @Override
  public final void setPersistValue(Attribute<?> attribute, boolean persistValue) {
    entityDefinition().property(attribute);
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
    return entity.entityType();
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void replaceForeignKeyValues(Collection<Entity> entities) {
    Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
      List<ForeignKey> foreignKeys = entityDefinition().foreignKeys(entityTypeEntities.getKey());
      for (ForeignKey foreignKey : foreignKeys) {
        replaceForeignKey(foreignKey, entityTypeEntities.getValue());
      }
    }
  }

  @Override
  public final Entity entityCopy() {
    return entity().deepCopy();
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
  public boolean isEntityNew() {
    return entity().isNew();
  }

  @Override
  public final void setForeignKeyValues(Collection<Entity> entities) {
    Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
      for (ForeignKey foreignKey : entityDefinition().foreignKeys(entityTypeEntities.getKey())) {
        //todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
        put(foreignKey, entityTypeEntities.getValue().iterator().next());
      }
    }
  }

  @Override
  public final void initialize(ForeignKey foreignKey, Entity foreignKeyValue) {
    requireNonNull(foreignKey);
    if (isEntityNew() && (foreignKeyValue != null || initializeForeignKeyToNull)) {
      put(foreignKey, foreignKeyValue);
    }
  }

  @Override
  public final <T> T get(Attribute<T> attribute) {
    return entity.get(attribute);
  }

  @Override
  public final <T> Optional<T> getOptional(Attribute<T> attribute) {
    return entity.getOptional(attribute);
  }

  @Override
  public final <T> void put(Attribute<T> attribute, T value) {
    requireNonNull(attribute, "attribute");
    Map<Attribute<?>, Object> dependingValues = getDependentValues(attribute);
    T previousValue = entity.put(attribute, value);
    if (!Objects.equals(value, previousValue)) {
      notifyValueEdit(attribute, value, dependingValues);
    }
  }

  @Override
  public final <T> T remove(Attribute<T> attribute) {
    requireNonNull(attribute, PROPERTY);
    T value = null;
    if (entity.contains(attribute)) {
      Map<Attribute<?>, Object> dependingValues = getDependentValues(attribute);
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
  public final void validate(Collection<Entity> entities) throws ValidationException {
    for (Entity entityToValidate : entities) {
      validate(entityToValidate);
    }
  }

  @Override
  public void validate(Entity entity) throws ValidationException {
    if (entity.entityType().equals(entityType())) {
      validator.validate(entity);
    }
    else {
      entity.entityDefinition().validator().validate(entity);
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
    Entity toInsert = entityCopy();
    if (entityDefinition().isKeyGenerated()) {
      toInsert.clearPrimaryKey();
    }
    toInsert.saveAll();
    List<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    doSetEntity(insertedEntities.get(0));

    notifyAfterInsert(unmodifiableList(insertedEntities));

    return insertedEntities.get(0);
  }

  @Override
  public final List<Entity> insert(List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    List<Entity> insertedEntities = insertEntities(entities);

    notifyAfterInsert(unmodifiableList(insertedEntities));

    return insertedEntities;
  }

  @Override
  public final Entity update() throws DatabaseException, ValidationException {
    List<Entity> updated = update(singletonList(entityCopy()));
    if (updated.isEmpty()) {
      throw new UpdateException("Active entity is not modified");
    }

    return updated.get(0);
  }

  @Override
  public final List<Entity> update(List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    LOG.debug("{} - update {}", this, entities);

    List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    notifyBeforeUpdate(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(entities)));
    validate(modifiedEntities);

    List<Entity> updatedEntities = doUpdate(modifiedEntities);
    int index = updatedEntities.indexOf(entity());
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    notifyAfterUpdate(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(updatedEntities)));

    return updatedEntities;
  }

  @Override
  public final Entity delete() throws DatabaseException {
    Entity originalEntity = entityCopy();
    originalEntity.revertAll();

    return delete(singletonList(originalEntity)).get(0);
  }

  @Override
  public final List<Entity> delete(List<Entity> entities) throws DatabaseException {
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Delete is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    LOG.debug("{} - delete {}", this, entities);

    notifyBeforeDelete(unmodifiableList(entities));

    List<Entity> deleted = doDelete(entities);
    if (deleted.contains(entity())) {
      doSetEntity(null);
    }

    notifyAfterDelete(unmodifiableList(deleted));

    return deleted;
  }

  @Override
  public final void refresh() {
    refreshDataModels();
    refreshEvent.onEvent();
  }

  @Override
  public final void refreshEntity() {
    try {
      if (!isEntityNew()) {
        setEntity(connectionProvider().connection().select(entity().primaryKey()));
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean containsSearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyProperty(foreignKey);
    synchronized (entitySearchModels) {
      return entitySearchModels.containsKey(foreignKey);
    }
  }

  @Override
  public EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey) {
    ForeignKeyProperty property = entityDefinition().foreignKeyProperty(foreignKey);
    Collection<Attribute<String>> searchAttributes = entities()
            .getDefinition(property.referencedEntityType()).searchAttributes();
    if (searchAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes defined for entity: " + property.referencedEntityType());
    }

    EntitySearchModel searchModel = new DefaultEntitySearchModel(property.referencedEntityType(), connectionProvider, searchAttributes);
    searchModel.multipleSelectionEnabledState().set(false);

    return searchModel;
  }

  @Override
  public final EntitySearchModel getForeignKeySearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyProperty(foreignKey);
    synchronized (entitySearchModels) {
      // can't use computeIfAbsent here, see comment in SwingEntityEditModel.getForeignKeyComboBoxModel()
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
    entityDefinition().property(attribute);
    return (Value<T>) editModelValues.computeIfAbsent(attribute, k -> new EditModelValue<>(this, attribute));
  }

  @Override
  public final boolean containsUnsavedData() {
    if (isEntityNew()) {
      EntityDefinition entityDefinition = entityDefinition();
      for (ColumnProperty<?> property : entityDefinition.columnProperties()) {
        if (!entityDefinition.isForeignKeyAttribute(property.attribute()) && valueModified(property.attribute())) {
          return true;
        }
      }
      for (ForeignKey foreignKey : entityDefinition.foreignKeys()) {
        if (valueModified(foreignKey)) {
          return true;
        }
      }

      return false;
    }

    return !entity().originalEntrySet().isEmpty();
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
  public final boolean isInitializeForeignKeyToNull() {
    return initializeForeignKeyToNull;
  }

  @Override
  public final void setInitializeForeignKeyToNull(boolean initializeForeignKeyToNull) {
    this.initializeForeignKeyToNull = initializeForeignKeyToNull;
  }

  @Override
  public final <T> void removeEditListener(Attribute<T> attribute, EventDataListener<T> listener) {
    if (valueEditEvents.containsKey(attribute)) {
      ((Event<T>) valueEditEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addEditListener(Attribute<T> attribute, EventDataListener<T> listener) {
    getEditEvent(attribute).addDataListener(listener);
  }

  @Override
  public final <T> void removeValueListener(Attribute<T> attribute, EventDataListener<T> listener) {
    if (valueChangeEvents.containsKey(attribute)) {
      ((Event<T>) valueChangeEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addValueListener(Attribute<T> attribute, EventDataListener<T> listener) {
    getValueEvent(attribute).addDataListener(listener);
  }

  @Override
  public final void removeValueListener(EventDataListener<Attribute<?>> listener) {
    valueChangeEvent.removeDataListener(listener);
  }

  @Override
  public final void addValueListener(EventDataListener<Attribute<?>> listener) {
    valueChangeEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntitySetListener(EventDataListener<Entity> listener) {
    entitySetEvent.removeDataListener(listener);
  }

  @Override
  public final void addEntitySetListener(EventDataListener<Entity> listener) {
    entitySetEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeInsertListener(EventDataListener<List<Entity>> listener) {
    beforeInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeInsertListener(EventDataListener<List<Entity>> listener) {
    beforeInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterInsertListener(EventDataListener<List<Entity>> listener) {
    afterInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterInsertListener(EventDataListener<List<Entity>> listener) {
    afterInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeUpdateListener(EventDataListener<Map<Key, Entity>> listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeUpdateListener(EventDataListener<Map<Key, Entity>> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterUpdateListener(EventDataListener<Map<Key, Entity>> listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterUpdateListener(EventDataListener<Map<Key, Entity>> listener) {
    afterUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void addBeforeDeleteListener(EventDataListener<List<Entity>> listener) {
    beforeDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeDeleteListener(EventDataListener<List<Entity>> listener) {
    beforeDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void removeAfterDeleteListener(EventDataListener<List<Entity>> listener) {
    afterDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterDeleteListener(EventDataListener<List<Entity>> listener) {
    afterDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntitiesEditedListener(EventListener listener) {
    entitiesEditedEvent.removeListener(listener);
  }

  @Override
  public final void addEntitiesEditedListener(EventListener listener) {
    entitiesEditedEvent.addListener(listener);
  }

  @Override
  public final void addConfirmSetEntityObserver(EventDataListener<State> listener) {
    confirmSetEntityEvent.addDataListener(listener);
  }

  @Override
  public final void removeConfirmSetEntityObserver(EventDataListener<State> listener) {
    confirmSetEntityEvent.removeDataListener(listener);
  }

  @Override
  public final void addRefreshListener(EventListener listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  /**
   * @return the actual {@link Entity} instance being edited
   */
  protected final Entity entity() {
    return entity;
  }

  /**
   * Inserts the given entities into the database
   * @param entities the entities to insert
   * @return a list containing the primary keys of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Key> doInsert(List<Entity> entities) throws DatabaseException {
    return connectionProvider.connection().insert(entities);
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Entity> doUpdate(List<Entity> entities) throws DatabaseException {
    return connectionProvider.connection().update(entities);
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @return a list containing the deleted entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Entity> doDelete(List<Entity> entities) throws DatabaseException {
    connectionProvider.connection().delete(Entity.getPrimaryKeys(entities));

    return entities;
  }

  /**
   * Called during the {@link #update()} function, to determine which entities need to be updated,
   * these entities will then be forwarded to {@link #doUpdate(java.util.List)}.
   * Returns the entities that have been modified and require updating, override to be able to
   * perform an update on unmodified entities or to return an empty list to veto an update action.
   * @param entities the entities
   * @return the entities requiring update
   * @see #update()
   * @see #update(java.util.List)
   */
  protected List<Entity> getModifiedEntities(List<Entity> entities) {
    return Entity.getModified(entities);
  }

  /**
   * Refresh all data-models used by this edit model, combo box models and such.
   */
  protected void refreshDataModels() {}

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code values}, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKey the foreign key attribute
   * @param values the foreign key entities
   */
  protected void replaceForeignKey(ForeignKey foreignKey, List<Entity> values) {
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
   * @see #addBeforeInsertListener(EventDataListener)
   */
  protected final void notifyBeforeInsert(List<Entity> entitiesToInsert) {
    beforeInsertEvent.onEvent(entitiesToInsert);
  }

  /**
   * Notifies that insert has been performed
   * @param insertedEntities the inserted entities
   * @see #addAfterInsertListener(EventDataListener)
   */
  protected final void notifyAfterInsert(List<Entity> insertedEntities) {
    afterInsertEvent.onEvent(insertedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyInserted(insertedEntities);
    }
  }

  /**
   * Notifies that update is about to be performed
   * @param entitiesToUpdate the entities about to be updated
   * @see #addBeforeUpdateListener(EventDataListener)
   */
  protected final void notifyBeforeUpdate(Map<Key, Entity> entitiesToUpdate) {
    beforeUpdateEvent.onEvent(entitiesToUpdate);
  }

  /**
   * Notifies that update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(EventDataListener)
   */
  protected final void notifyAfterUpdate(Map<Key, Entity> updatedEntities) {
    afterUpdateEvent.onEvent(updatedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyUpdated(updatedEntities);
    }
  }

  /**
   * Notifies that delete is about to be performed
   * @param entitiesToDelete the entities about to be deleted
   * @see #addBeforeDeleteListener(EventDataListener)
   */
  protected final void notifyBeforeDelete(List<Entity> entitiesToDelete) {
    beforeDeleteEvent.onEvent(entitiesToDelete);
  }

  /**
   * Notifies that delete has been performed
   * @param deletedEntities the deleted entities
   * @see #addAfterDeleteListener(EventDataListener)
   */
  protected final void notifyAfterDelete(List<Entity> deletedEntities) {
    afterDeleteEvent.onEvent(deletedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyDeleted(deletedEntities);
    }
  }

  private List<Entity> insertEntities(List<Entity> entities) throws DatabaseException, ValidationException {
    LOG.debug("{} - insert {}", this, entities.toString());
    notifyBeforeInsert(unmodifiableList(entities));
    validate(entities);

    return connectionProvider.connection().select(doInsert(entities));
  }

  private boolean isSetEntityAllowed() {
    if (warnAboutUnsavedData && containsUnsavedData()) {
      State confirmation = State.state(true);
      confirmSetEntityEvent.onEvent(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void doSetEntity(Entity entity) {
    Map<Attribute<?>, Object> affectedAttributes = this.entity.setAs(entity == null ? getDefaultEntity(this::getDefaultValue) : entity);
    for (Map.Entry<Attribute<?>, Object> entry : affectedAttributes.entrySet()) {
      Attribute<Object> objectAttribute = (Attribute<Object>) entry.getKey();
      onValueChange(objectAttribute, this.entity.get(objectAttribute));
    }
    if (affectedAttributes.isEmpty()) {//no value changes to trigger state updates
      updateEntityStates();
    }

    entitySetEvent.onEvent(entity);
  }

  private boolean valueModified(Attribute<?> attribute) {
    return !Objects.equals(get(attribute), getDefaultValue(attribute));
  }

  private <T> Event<T> getEditEvent(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    return (Event<T>) valueEditEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private <T> Event<T> getValueEvent(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    return (Event<T>) valueChangeEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private void initializePersistentValues() {
    if (EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get()) {
      entityDefinition().foreignKeys().forEach(foreignKey -> setPersistValue(foreignKey, true));
    }
  }

  /**
   * Instantiates a new {@link Entity} using the values provided by {@code valueSupplier}.
   * Values are populated for {@link ColumnProperty} and its descendants, {@link ForeignKeyProperty}
   * and {@link TransientProperty} (excluding its descendants).
   * If a {@link ColumnProperty}s underlying column has a default value the property is
   * skipped unless the property itself has a default value, which then overrides the columns default value.
   * @return an entity instance populated with default values
   * @see ColumnProperty.Builder#columnHasDefaultValue()
   * @see ColumnProperty.Builder#defaultValue(Object)
   */
  private Entity getDefaultEntity(ValueSupplier valueSupplier) {
    EntityDefinition definition = entityDefinition();
    Entity newEntity = definition.entity();
    for (@SuppressWarnings("rawtypes") ColumnProperty property : definition.columnProperties()) {
      if (!definition.isForeignKeyAttribute(property.attribute()) && !property.denormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        newEntity.put(property.attribute(), valueSupplier.get(property));
      }
    }
    for (@SuppressWarnings("rawtypes") TransientProperty transientProperty : definition.transientProperties()) {
      if (!(transientProperty instanceof DerivedProperty)) {
        newEntity.put(transientProperty.attribute(), valueSupplier.get(transientProperty));
      }
    }
    for (ForeignKeyProperty foreignKeyProperty : definition.foreignKeyProperties()) {
      newEntity.put(foreignKeyProperty.attribute(), valueSupplier.get(foreignKeyProperty));
    }
    newEntity.saveAll();

    return newEntity;
  }

  private <T> T getDefaultValue(Attribute<T> attribute) {
    return getDefaultValue(entityDefinition().property(attribute));
  }

  private <T> T getDefaultValue(Property<T> property) {
    if (isPersistValue(property.attribute())) {
      if (property instanceof ForeignKeyProperty) {
        return (T) entity.referencedEntity((ForeignKey) property.attribute());
      }

      return entity.get(property.attribute());
    }

    return (T) defaultValueSuppliers.computeIfAbsent(property.attribute(), k -> property::defaultValue).get();
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesEditedEvent);
    afterInsertEvent.addListener(entitiesEditedEvent);
    afterUpdateEvent.addListener(entitiesEditedEvent);
  }

  private Map<Attribute<?>, Object> getDependentValues(Attribute<?> attribute) {
    Map<Attribute<?>, Object> dependentValues = new HashMap<>();
    EntityDefinition entityDefinition = entityDefinition();
    entityDefinition.derivedAttributes(attribute).forEach(derivedAttribute ->
            dependentValues.put(derivedAttribute, get(derivedAttribute)));
    entityDefinition.foreignKeyProperties(attribute).forEach(foreignKeyProperty ->
            dependentValues.put(foreignKeyProperty.attribute(), get(foreignKeyProperty.attribute())));
    if (attribute instanceof ForeignKey) {
      ((ForeignKey) attribute).references().forEach(reference ->
              dependentValues.put(reference.attribute(), get(reference.attribute())));
    }

    return dependentValues;
  }

  private <T> void notifyValueEdit(Attribute<T> attribute, T value, Map<Attribute<?>, Object> dependentValues) {
    onValueChange(attribute, value);
    getEditEvent(attribute).onEvent(value);
    dependentValues.forEach((dependentAttribute, previousValue) -> {
      Object currentValue = get(dependentAttribute);
      if (!Objects.equals(previousValue, currentValue)) {
        notifyValueEdit((Attribute<Object>) dependentAttribute, currentValue, emptyMap());
      }
    });
  }

  private <T> void onValueChange(Attribute<T> attribute, T value) {
    updateEntityStates();
    Event<T> changeEvent = (Event<T>) valueChangeEvents.get(attribute);
    if (changeEvent != null) {
      changeEvent.onEvent(value);
    }
    valueChangeEvent.onEvent(attribute);
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
  private static Map<Key, Entity> mapToOriginalPrimaryKey(List<Entity> entitiesBeforeUpdate,
                                                          List<Entity> entitiesAfterUpdate) {
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
    <T> T get(Property<T> property);
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