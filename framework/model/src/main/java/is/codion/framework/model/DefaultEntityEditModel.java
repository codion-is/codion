/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  private final Event<?> beforeRefreshEvent = Event.event();
  private final Event<?> afterRefreshEvent = Event.event();
  private final Event<State> confirmSetEntityEvent = Event.event();

  private final State entityModifiedState = State.state();
  private final State primaryKeyNullState = State.state(true);
  private final State insertEnabledState = State.state(true);
  private final State updateEnabledState = State.state(true);
  private final State deleteEnabledState = State.state(true);
  private final StateObserver readOnlyState = State.and(insertEnabledState.getReversedObserver(),
          updateEnabledState.getReversedObserver(), deleteEnabledState.getReversedObserver());

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
  private final Map<ForeignKey, EntitySearchModel> entitySearchModels = new ConcurrentHashMap<>();

  /**
   * Holds the edit model values created via {@link #value(Attribute)}
   */
  private final Map<Attribute<?>, Value<?>> editModelValues = new HashMap<>();

  /**
   * Contains true if values should persist for the given property when the model is cleared
   */
  private final Map<Attribute<?>, Boolean> persistentValues = new HashMap<>();

  /**
   * Fired when the active entity is set.
   * @see #setEntity(Entity)
   */
  private final Event<Entity> entitySetEvent = Event.event();

  /**
   * An event notified each time a value changes
   */
  private final Event<ValueChange<?>> valueChangeEvent = Event.event();

  /**
   * The validator used by this edit model
   */
  private final EntityValidator validator;

  /**
   * A state indicating whether the entity being edited is in a valid state according the validator
   */
  private final State validState = State.state();

  /**
   * Holds events signaling value changes made via {@link #put(Attribute, Object)} or {@link #remove(Attribute)}
   */
  private final Map<Attribute<?>, Event<?>> valueEditEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes in the underlying {@link Entity}
   */
  private final Map<Attribute<?>, Event<?>> valueChangeEventMap = new HashMap<>();

  /**
   * Holds the default value suppliers for attributes
   */
  private final Map<Attribute<?>, Supplier<?>> defaultValueSupplierMap = new HashMap<>();

  /**
   * A state indicating whether the entity being edited is new
   * @see #isEntityNew()
   */
  private final State entityNewState = State.state(true);

  /**
   * Provides whether the underlying entity is in a modified state
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
   * Instantiates a new {@link DefaultEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public DefaultEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getValidator());
  }

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the entityTypeentified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                final EntityValidator validator) {
    this.entity = connectionProvider.getEntities().entity(entityType);
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.validator = validator;
    this.modifiedSupplier = entity::isModified;
    setReadOnly(getEntityDefinition().isReadOnly());
    initializePersistentValues();
    bindEventsInternal();
    doSetEntity(getDefaultEntity(Property::getDefaultValue));
  }

  @Override
  public final Entities getEntities() {
    return connectionProvider.getEntities();
  }

  @Override
  public final EntityDefinition getEntityDefinition() {
    return getEntities().getDefinition(entity.getEntityType());
  }

  @Override
  public final String toString() {
    return getClass().toString() + ", " + entity.getEntityType();
  }

  @Override
  public final <T> void setDefaultValueSupplier(final Attribute<T> attribute, final Supplier<T> valueSupplier) {
    requireNonNull(valueSupplier, "valueSupplier");
    getEntityDefinition().getProperty(attribute);
    defaultValueSupplierMap.put(attribute, valueSupplier);
  }

  @Override
  public final void setModifiedSupplier(final Supplier<Boolean> modifiedSupplier) {
    this.modifiedSupplier = requireNonNull(modifiedSupplier, "isModifiedSupplier");
  }

  @Override
  public final boolean isReadOnly() {
    return readOnlyState.get();
  }

  @Override
  public final void setReadOnly(final boolean readOnly) {
    insertEnabledState.set(!readOnly);
    updateEnabledState.set(!readOnly);
    deleteEnabledState.set(!readOnly);
  }

  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  @Override
  public final void setWarnAboutUnsavedData(final boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
  }

  @Override
  public boolean isPersistValue(final Attribute<?> attribute) {
    getEntityDefinition().getProperty(attribute);

    return Boolean.TRUE.equals(persistentValues.get(attribute));
  }

  @Override
  public final void setPersistValue(final Attribute<?> attribute, final boolean persistValue) {
    getEntityDefinition().getProperty(attribute);
    persistentValues.put(attribute, persistValue);
  }

  @Override
  public final boolean isInsertEnabled() {
    return insertEnabledState.get();
  }

  @Override
  public final void setInsertEnabled(final boolean insertEnabled) {
    insertEnabledState.set(insertEnabled);
  }

  @Override
  public final StateObserver getInsertEnabledObserver() {
    return insertEnabledState.getObserver();
  }

  @Override
  public final boolean isUpdateEnabled() {
    return updateEnabledState.get();
  }

  @Override
  public final void setUpdateEnabled(final boolean updateEnabled) {
    updateEnabledState.set(updateEnabled);
  }

  @Override
  public final StateObserver getUpdateEnabledObserver() {
    return updateEnabledState.getObserver();
  }

  @Override
  public final boolean isDeleteEnabled() {
    return deleteEnabledState.get();
  }

  @Override
  public final void setDeleteEnabled(final boolean deleteEnabled) {
    deleteEnabledState.set(deleteEnabled);
  }

  @Override
  public final StateObserver getDeleteEnabledObserver() {
    return deleteEnabledState.getObserver();
  }

  @Override
  public final StateObserver getEntityNewObserver() {
    return entityNewState.getObserver();
  }

  @Override
  public final StateObserver getPrimaryKeyNullObserver() {
    return primaryKeyNullState.getObserver();
  }

  @Override
  public final void setEntity(final Entity entity) {
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
  public final EntityType getEntityType() {
    return entity.getEntityType();
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void replaceForeignKeyValues(final Collection<Entity> entities) {
    final Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
      final List<ForeignKey> foreignKeys = getEntityDefinition().getForeignKeys(entityTypeEntities.getKey());
      for (final ForeignKey foreignKey : foreignKeys) {
        replaceForeignKey(foreignKey, entityTypeEntities.getValue());
      }
    }
  }

  @Override
  public final Entity getEntityCopy() {
    return getEntity().deepCopy();
  }

  @Override
  public final Entity getForeignKey(final ForeignKey foreignKey) {
    return entity.get(foreignKey);
  }

  @Override
  public StateObserver getModifiedObserver() {
    return entityModifiedState.getObserver();
  }

  @Override
  public final boolean isModified() {
    return modifiedSupplier.get();
  }

  @Override
  public boolean isEntityNew() {
    return getEntity().isNew();
  }

  @Override
  public final void setForeignKeyValues(final Collection<Entity> entities) {
    final Map<EntityType, List<Entity>> entitiesByEntityType = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entityTypeEntities : entitiesByEntityType.entrySet()) {
      for (final ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entityTypeEntities.getKey())) {
        //todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
        put(foreignKey, entityTypeEntities.getValue().iterator().next());
      }
    }
  }

  @Override
  public final void initialize(final ForeignKey foreignKey, final Entity foreignKeyValue) {
    requireNonNull(foreignKey);
    if (isEntityNew() && (foreignKeyValue != null || initializeForeignKeyToNull)) {
      put(foreignKey, foreignKeyValue);
    }
  }

  @Override
  public final <T> T get(final Attribute<T> attribute) {
    return entity.get(attribute);
  }

  @Override
  public final <T> Optional<T> getOptional(final Attribute<T> attribute) {
    return entity.getOptional(attribute);
  }

  @Override
  public final <T> void put(final Attribute<T> attribute, final T value) {
    requireNonNull(attribute, "attribute");
    final Map<Attribute<?>, Object> dependingValues = getDependentValues(attribute);
    final T previousValue = entity.put(attribute, value);
    if (!Objects.equals(value, previousValue)) {
      notifyValueEdit(new DefaultValueChange<>(attribute, value, previousValue), dependingValues);
    }
  }

  @Override
  public final <T> T remove(final Attribute<T> attribute) {
    requireNonNull(attribute, PROPERTY);
    T value = null;
    if (entity.contains(attribute)) {
      final Map<Attribute<?>, Object> dependingValues = getDependentValues(attribute);
      value = entity.remove(attribute);
      notifyValueEdit(new DefaultValueChange<>(attribute, null, value), dependingValues);
    }

    return value;
  }

  @Override
  public final boolean isNullable(final Attribute<?> attribute) {
    return validator.isNullable(entity, getEntityDefinition().getProperty(attribute));
  }

  @Override
  public final boolean isNull(final Attribute<?> attribute) {
    return entity.isNull(attribute);
  }

  @Override
  public final boolean isNotNull(final Attribute<?> attribute) {
    return !entity.isNull(attribute);
  }

  @Override
  public final StateObserver getValidObserver() {
    return validState.getObserver();
  }

  @Override
  public final boolean isValid() {
    return validState.get();
  }

  @Override
  public final void validate(final Attribute<?> attribute) throws ValidationException {
    final EntityDefinition entityDefinition = getEntityDefinition();
    validator.validate(entity, entityDefinition, entityDefinition.getProperty(attribute));
  }

  @Override
  public final void validate() throws ValidationException {
    validate(entity);
  }

  @Override
  public final void validate(final Collection<Entity> entities) throws ValidationException {
    for (final Entity entityToValidate : entities) {
      validate(entityToValidate);
    }
  }

  @Override
  public void validate(final Entity entity) throws ValidationException {
    final EntityDefinition definition = getEntities().getDefinition(entity.getEntityType());
    if (definition.getEntityType().equals(getEntityType())) {
      validator.validate(entity, definition);
    }
    else {
      definition.getValidator().validate(entity, definition);
    }
  }

  @Override
  public final boolean isValid(final Attribute<?> attribute) {
    try {
      validate(attribute);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  @Override
  public final EntityValidator getValidator() {
    return validator;
  }

  @Override
  public final Entity insert() throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    final Entity toInsert = getEntityCopy();
    if (getEntityDefinition().isKeyGenerated()) {
      toInsert.clearPrimaryKey();
    }
    toInsert.saveAll();
    final List<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    doSetEntity(insertedEntities.get(0));

    notifyAfterInsert(unmodifiableList(insertedEntities));

    return insertedEntities.get(0);
  }

  @Override
  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    final List<Entity> insertedEntities = insertEntities(entities);

    notifyAfterInsert(unmodifiableList(insertedEntities));

    return insertedEntities;
  }

  @Override
  public final Entity update() throws DatabaseException, ValidationException {
    final List<Entity> updated = update(singletonList(getEntityCopy()));
    if (updated.isEmpty()) {
      throw new UpdateException("Active entity is not modified");
    }

    return updated.get(0);
  }

  @Override
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    LOG.debug("{} - update {}", this, entities);

    final List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    notifyBeforeUpdate(unmodifiableMap(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(entities))));
    validate(modifiedEntities);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    notifyAfterUpdate(unmodifiableMap(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(updatedEntities))));

    return updatedEntities;
  }

  @Override
  public final Entity delete() throws DatabaseException {
    final Entity originalEntity = getEntityCopy();
    originalEntity.revertAll();

    return delete(singletonList(originalEntity)).get(0);
  }

  @Override
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException {
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Delete is not enabled!");
    }
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    LOG.debug("{} - delete {}", this, entities);

    notifyBeforeDelete(unmodifiableList(entities));

    final List<Entity> deleted = doDelete(entities);
    if (deleted.contains(getEntity())) {
      doSetEntity(null);
    }

    notifyAfterDelete(unmodifiableList(deleted));

    return deleted;
  }

  @Override
  public final void refresh() {
    try {
      beforeRefreshEvent.onEvent();
      refreshDataModels();
    }
    finally {
      afterRefreshEvent.onEvent();
    }
  }

  @Override
  public final void refreshEntity() {
    try {
      if (!isEntityNew()) {
        setEntity(getConnectionProvider().getConnection().selectSingle(getEntity().getPrimaryKey()));
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean containsSearchModel(final ForeignKey foreignKey) {
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    return entitySearchModels.containsKey(foreignKey);
  }

  @Override
  public EntitySearchModel createForeignKeySearchModel(final ForeignKey foreignKey) {
    final ForeignKeyProperty property = getEntityDefinition().getForeignKeyProperty(foreignKey);
    final Collection<Attribute<String>> searchAttributes = getEntities()
            .getDefinition(property.getReferencedEntityType()).getSearchAttributes();
    if (searchAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes defined for entity: " + property.getReferencedEntityType());
    }

    final EntitySearchModel searchModel = new DefaultEntitySearchModel(property.getReferencedEntityType(), connectionProvider, searchAttributes);
    searchModel.getMultipleSelectionEnabledValue().set(false);

    return searchModel;
  }

  @Override
  public final EntitySearchModel getForeignKeySearchModel(final ForeignKey foreignKey) {
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    synchronized (entitySearchModels) {
      EntitySearchModel searchModel = entitySearchModels.get(foreignKey);
      if (searchModel == null) {
        searchModel = createForeignKeySearchModel(foreignKey);
        entitySearchModels.put(foreignKey, searchModel);
      }

      return searchModel;
    }
  }

  @Override
  public final <T> Value<T> value(final Attribute<T> attribute) {
    getEntityDefinition().getProperty(attribute);
    return (Value<T>) editModelValues.computeIfAbsent(attribute,
            valueAttribute -> new EditModelValue<>(this, attribute));
  }

  @Override
  public final boolean containsUnsavedData() {
    if (isEntityNew()) {
      final EntityDefinition entityDefinition = getEntityDefinition();
      for (final ColumnProperty<?> property : entityDefinition.getColumnProperties()) {
        if (!entityDefinition.isForeignKeyAttribute(property.getAttribute()) && valueModified(property.getAttribute())) {
          return true;
        }
      }
      for (final ForeignKey foreignKey : entityDefinition.getForeignKeys()) {
        if (valueModified(foreignKey)) {
          return true;
        }
      }

      return false;
    }

    return !getEntity().originalEntrySet().isEmpty();
  }

  @Override
  public final boolean isPostEditEvents() {
    return postEditEvents;
  }

  @Override
  public final void setPostEditEvents(final boolean postEditEvents) {
    this.postEditEvents = postEditEvents;
  }

  @Override
  public final boolean isInitializeForeignKeyToNull() {
    return initializeForeignKeyToNull;
  }

  @Override
  public final void setInitializeForeignKeyToNull(final boolean initializeForeignKeyToNull) {
    this.initializeForeignKeyToNull = initializeForeignKeyToNull;
  }

  @Override
  public final <T> void removeValueEditListener(final Attribute<T> attribute, final EventDataListener<ValueChange<T>> listener) {
    if (valueEditEventMap.containsKey(attribute)) {
      ((Event<ValueChange<T>>) valueEditEventMap.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addValueEditListener(final Attribute<T> attribute, final EventDataListener<ValueChange<T>> listener) {
    getValueEditEvent(attribute).addDataListener(listener);
  }

  @Override
  public final <T> void removeValueListener(final Attribute<T> attribute, final EventDataListener<ValueChange<T>> listener) {
    if (valueChangeEventMap.containsKey(attribute)) {
      ((Event<ValueChange<T>>) valueChangeEventMap.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addValueListener(final Attribute<T> attribute, final EventDataListener<ValueChange<T>> listener) {
    getValueChangeEvent(attribute).addDataListener(listener);
  }

  @Override
  public final void removeValueListener(final EventDataListener<ValueChange<?>> listener) {
    valueChangeEvent.removeDataListener(listener);
  }

  @Override
  public final void addValueListener(final EventDataListener<ValueChange<?>> listener) {
    valueChangeEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntitySetListener(final EventDataListener<Entity> listener) {
    entitySetEvent.removeDataListener(listener);
  }

  @Override
  public final void addEntitySetListener(final EventDataListener<Entity> listener) {
    entitySetEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeInsertListener(final EventDataListener<List<Entity>> listener) {
    beforeInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeInsertListener(final EventDataListener<List<Entity>> listener) {
    beforeInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterInsertListener(final EventDataListener<List<Entity>> listener) {
    afterInsertEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterInsertListener(final EventDataListener<List<Entity>> listener) {
    afterInsertEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeUpdateListener(final EventDataListener<Map<Key, Entity>> listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeUpdateListener(final EventDataListener<Map<Key, Entity>> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterUpdateListener(final EventDataListener<Map<Key, Entity>> listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterUpdateListener(final EventDataListener<Map<Key, Entity>> listener) {
    afterUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void addBeforeDeleteListener(final EventDataListener<List<Entity>> listener) {
    beforeDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeBeforeDeleteListener(final EventDataListener<List<Entity>> listener) {
    beforeDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void removeAfterDeleteListener(final EventDataListener<List<Entity>> listener) {
    afterDeleteEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterDeleteListener(final EventDataListener<List<Entity>> listener) {
    afterDeleteEvent.addDataListener(listener);
  }

  @Override
  public final void removeEntitiesEditedListener(final EventListener listener) {
    entitiesEditedEvent.removeListener(listener);
  }

  @Override
  public final void addEntitiesEditedListener(final EventListener listener) {
    entitiesEditedEvent.addListener(listener);
  }

  @Override
  public final void addBeforeRefreshListener(final EventListener listener) {
    beforeRefreshEvent.addListener(listener);
  }

  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    beforeRefreshEvent.removeListener(listener);
  }

  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.addListener(listener);
  }

  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.removeListener(listener);
  }

  @Override
  public void addConfirmSetEntityObserver(final EventDataListener<State> listener) {
    confirmSetEntityEvent.addDataListener(listener);
  }

  @Override
  public void removeConfirmSetEntityObserver(final EventDataListener<State> listener) {
    confirmSetEntityEvent.removeDataListener(listener);
  }

  /**
   * @return the actual {@link Entity} instance being edited
   */
  protected final Entity getEntity() {
    return entity;
  }

  /**
   * Inserts the given entities into the database
   * @param entities the entities to insert
   * @return a list containing the primary keys of the inserted entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Key> doInsert(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().insert(entities);
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Entity> doUpdate(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().update(entities);
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @return a list containing the deleted entities
   * @throws DatabaseException in case of a database exception
   */
  protected List<Entity> doDelete(final List<Entity> entities) throws DatabaseException {
    connectionProvider.getConnection().delete(Entity.getPrimaryKeys(entities));

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
  protected List<Entity> getModifiedEntities(final List<Entity> entities) {
    return Entity.getModified(entities);
  }

  protected void refreshDataModels() {}

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code values}, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKey the foreign key attribute
   * @param values the foreign key entities
   */
  protected void replaceForeignKey(final ForeignKey foreignKey, final List<Entity> values) {
    final Entity currentForeignKeyValue = getForeignKey(foreignKey);
    if (currentForeignKeyValue != null) {
      for (final Entity replacementValue : values) {
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
  protected final State getModifiedState() {
    return entityModifiedState;
  }

  /**
   * Notifies that insert is about to be performed
   * @param entitiesToInsert the entities about to be inserted
   * @see #addBeforeInsertListener(EventDataListener)
   */
  protected final void notifyBeforeInsert(final List<Entity> entitiesToInsert) {
    beforeInsertEvent.onEvent(entitiesToInsert);
  }

  /**
   * Notifies that insert has been performed
   * @param insertedEntities the inserted entities
   * @see #addAfterInsertListener(EventDataListener)
   */
  protected final void notifyAfterInsert(final List<Entity> insertedEntities) {
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
  protected final void notifyBeforeUpdate(final Map<Key, Entity> entitiesToUpdate) {
    beforeUpdateEvent.onEvent(entitiesToUpdate);
  }

  /**
   * Notifies that update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(EventDataListener)
   */
  protected final void notifyAfterUpdate(final Map<Key, Entity> updatedEntities) {
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
  protected final void notifyBeforeDelete(final List<Entity> entitiesToDelete) {
    beforeDeleteEvent.onEvent(entitiesToDelete);
  }

  /**
   * Notifies that delete has been performed
   * @param deletedEntities the deleted entities
   * @see #addAfterDeleteListener(EventDataListener)
   */
  protected final void notifyAfterDelete(final List<Entity> deletedEntities) {
    afterDeleteEvent.onEvent(deletedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyDeleted(deletedEntities);
    }
  }

  private List<Entity> insertEntities(final List<Entity> entities) throws DatabaseException, ValidationException {
    LOG.debug("{} - insert {}", this, entities.toString());
    notifyBeforeInsert(unmodifiableList(entities));
    validate(entities);

    return connectionProvider.getConnection().select(doInsert(entities));
  }

  private boolean isSetEntityAllowed() {
    if (warnAboutUnsavedData && containsUnsavedData()) {
      final State confirmation = State.state(true);
      confirmSetEntityEvent.onEvent(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void doSetEntity(final Entity entity) {
    final Map<Attribute<?>, Object> affectedAttributes = this.entity.setAs(entity == null ? getDefaultEntity(this::getDefaultValue) : entity);
    for (final Map.Entry<Attribute<?>, Object> entry : affectedAttributes.entrySet()) {
      final Attribute<Object> objectAttribute = (Attribute<Object>) entry.getKey();
      onValueChange(new DefaultValueChange<>(objectAttribute, this.entity.get(objectAttribute), entry.getValue()));
    }
    if (affectedAttributes.isEmpty()) {//no value changes to trigger state updates
      updateEntityStates();
    }

    entitySetEvent.onEvent(entity);
  }

  private boolean valueModified(final Attribute<?> attribute) {
    return !Objects.equals(get(attribute), getDefaultValue(attribute));
  }

  private <T> Event<ValueChange<T>> getValueEditEvent(final Attribute<T> attribute) {
    getEntityDefinition().getProperty(attribute);
    return (Event<ValueChange<T>>) valueEditEventMap.computeIfAbsent(attribute, k -> Event.event());
  }

  private <T> Event<ValueChange<T>> getValueChangeEvent(final Attribute<T> attribute) {
    getEntityDefinition().getProperty(attribute);
    return (Event<ValueChange<T>>) valueChangeEventMap.computeIfAbsent(attribute, k -> Event.event());
  }

  private void initializePersistentValues() {
    if (EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get()) {
      getEntityDefinition().getForeignKeys().forEach(foreignKey -> setPersistValue(foreignKey, true));
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
  private Entity getDefaultEntity(final ValueSupplier valueSupplier) {
    final EntityDefinition definition = getEntityDefinition();
    final Entity newEntity = definition.entity();
    for (@SuppressWarnings("rawtypes") final ColumnProperty property : definition.getColumnProperties()) {
      if (!definition.isForeignKeyAttribute(property.getAttribute()) && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        newEntity.put(property.getAttribute(), valueSupplier.get(property));
      }
    }
    for (@SuppressWarnings("rawtypes") final TransientProperty transientProperty : definition.getTransientProperties()) {
      if (!(transientProperty instanceof DerivedProperty)) {
        newEntity.put(transientProperty.getAttribute(), valueSupplier.get(transientProperty));
      }
    }
    for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
      newEntity.put(foreignKeyProperty.getAttribute(), valueSupplier.get(foreignKeyProperty));
    }
    newEntity.saveAll();

    return newEntity;
  }

  private <T> T getDefaultValue(final Attribute<T> attribute) {
    return getDefaultValue(getEntityDefinition().getProperty(attribute));
  }

  private <T> T getDefaultValue(final Property<T> property) {
    if (isPersistValue(property.getAttribute())) {
      if (property instanceof ForeignKeyProperty) {
        return (T) entity.getForeignKey((ForeignKey) property.getAttribute());
      }

      return entity.get(property.getAttribute());
    }

    return (T) defaultValueSupplierMap.computeIfAbsent(property.getAttribute(), a -> property::getDefaultValue).get();
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesEditedEvent);
    afterInsertEvent.addListener(entitiesEditedEvent);
    afterUpdateEvent.addListener(entitiesEditedEvent);
  }

  private Map<Attribute<?>, Object> getDependentValues(final Attribute<?> attribute) {
    final Map<Attribute<?>, Object> dependentValues = new HashMap<>();
    final EntityDefinition entityDefinition = getEntityDefinition();
    entityDefinition.getDerivedAttributes(attribute).forEach(derivedAttribute ->
            dependentValues.put(derivedAttribute, get(derivedAttribute)));
    entityDefinition.getForeignKeyProperties(attribute).forEach(foreignKeyProperty ->
            dependentValues.put(foreignKeyProperty.getAttribute(), get(foreignKeyProperty.getAttribute())));
    if (attribute instanceof ForeignKey) {
      ((ForeignKey) attribute).getReferences().forEach(reference ->
              dependentValues.put(reference.getAttribute(), get(reference.getAttribute())));
    }

    return dependentValues;
  }

  private <T> void notifyValueEdit(final ValueChange<T> valueChange, final Map<Attribute<?>, Object> dependentValues) {
    onValueChange(valueChange);
    getValueEditEvent(valueChange.getAttribute()).onEvent(valueChange);
    dependentValues.forEach((dependentAttribute, previousValue) -> {
      final Object currentValue = get(dependentAttribute);
      if (!Objects.equals(previousValue, currentValue)) {
        notifyValueEdit(new DefaultValueChange<>((Attribute<Object>) dependentAttribute, currentValue, previousValue), emptyMap());
      }
    });
  }

  private <T> void onValueChange(final ValueChange<T> valueChange) {
    updateEntityStates();
    final Event<ValueChange<T>> changeEvent = (Event<ValueChange<T>>) valueChangeEventMap.get(valueChange.getAttribute());
    if (changeEvent != null) {
      changeEvent.onEvent(valueChange);
    }
    valueChangeEvent.onEvent(valueChange);
  }

  private void updateEntityStates() {
    entityModifiedState.set(isModified());
    validState.set(validator.isValid(entity, getEntityDefinition()));
    primaryKeyNullState.set(entity.getPrimaryKey().isNull());
    entityNewState.set(isEntityNew());
  }

  /**
   * Maps the given entities and their updated counterparts to their original primary keys,
   * assumes a single copy of each entity in the given lists.
   * @param entitiesBeforeUpdate the entities before update
   * @param entitiesAfterUpdate the entities after update
   * @return the updated entities mapped to their respective original primary keys
   */
  private static Map<Key, Entity> mapToOriginalPrimaryKey(final List<Entity> entitiesBeforeUpdate,
                                                          final List<Entity> entitiesAfterUpdate) {
    final List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
    final Map<Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
    for (final Entity entity : entitiesBeforeUpdate) {
      keyMap.put(entity.getOriginalPrimaryKey(), findAndRemove(entity.getPrimaryKey(), entitiesAfterUpdateCopy.listIterator()));
    }

    return keyMap;
  }

  private static Entity findAndRemove(final Key primaryKey, final ListIterator<Entity> iterator) {
    while (iterator.hasNext()) {
      final Entity current = iterator.next();
      if (current.getPrimaryKey().equals(primaryKey)) {
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

    private EditModelValue(final EntityEditModel editModel, final Attribute<T> attribute) {
      this.editModel = editModel;
      this.attribute = attribute;
      this.editModel.addValueListener(attribute, valueChange -> notifyValueChange());
    }

    @Override
    public T get() {
      return editModel.get(attribute);
    }

    @Override
    protected void setValue(final T value) {
      editModel.put(attribute, value);
    }
  }
}