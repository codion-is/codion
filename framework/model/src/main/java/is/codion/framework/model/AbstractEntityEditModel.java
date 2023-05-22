/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
public abstract class AbstractEntityEditModel implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityEditModel.class);

  private static final String ENTITIES = "entities";

  private final Event<List<Entity>> beforeInsertEvent = Event.event();
  private final Event<List<Entity>> afterInsertEvent = Event.event();
  private final Event<Map<Key, Entity>> beforeUpdateEvent = Event.event();
  private final Event<Map<Key, Entity>> afterUpdateEvent = Event.event();
  private final Event<List<Entity>> beforeDeleteEvent = Event.event();
  private final Event<List<Entity>> afterDeleteEvent = Event.event();
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
  private final StateObserver readOnlyObserver = State.and(insertEnabledState.reversedObserver(),
          updateEnabledState.reversedObserver(), deleteEnabledState.reversedObserver());
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
   * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.entities().definition(entityType).validator());
  }

  /**
   * Instantiates a new {@link AbstractEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  protected AbstractEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                    EntityValidator validator) {
    this.entity = connectionProvider.entities().entity(entityType);
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.validator = requireNonNull(validator);
    this.modifiedSupplier = entity::isModified;
    setReadOnly(entityDefinition().isReadOnly());
    configurePersistentForeignKeyValues();
    bindEventsInternal();
    doSetEntity(defaultEntity(Property::defaultValue));
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
    return getClass().toString() + ", " + entity.type();
  }

  @Override
  public final <T> void setDefaultValueSupplier(Attribute<T> attribute, Supplier<T> valueSupplier) {
    entityDefinition().property(attribute);
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
    entityDefinition().property(attribute);

    return attributeModifiedStateMap.computeIfAbsent(attribute, k ->
            State.state(!entity.isNew() && entity.isModified(attribute))).observer();
  }

  @Override
  public final StateObserver nullObserver(Attribute<?> attribute) {
    entityDefinition().property(attribute);

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
  public final <T> void put(Attribute<T> attribute, T value) {
    entityDefinition().property(attribute);
    Map<Attribute<?>, Object> dependingValues = dependentValues(attribute);
    T previousValue = entity.put(attribute, value);
    if (!Objects.equals(value, previousValue)) {
      notifyValueEdit(attribute, value, dependingValues);
    }
  }

  @Override
  public final <T> T remove(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    T value = null;
    if (entity.contains(attribute)) {
      Map<Attribute<?>, Object> dependingValues = dependentValues(attribute);
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
    List<Entity> updated = update(singletonList(entity()));
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

    List<Entity> modifiedEntities = modifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    notifyBeforeUpdate(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(entities)));
    validate(modifiedEntities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - update {}", this, entities);

    List<Entity> updatedEntities = doUpdate(modifiedEntities);
    int index = updatedEntities.indexOf(entity);
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    notifyAfterUpdate(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(updatedEntities)));

    return updatedEntities;
  }

  @Override
  public final Entity delete() throws DatabaseException {
    Entity originalEntity = entity.copy();
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
    if (deleted.contains(entity)) {
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
        setEntity(connectionProvider().connection().select(entity.primaryKey()));
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
            .definition(property.referencedType()).searchAttributes();
    if (searchAttributes.isEmpty()) {
      throw new IllegalStateException("No search attributes defined for entity: " + property.referencedType());
    }

    EntitySearchModel searchModel = new DefaultEntitySearchModel(property.referencedType(), connectionProvider, searchAttributes);
    searchModel.multipleSelectionEnabledState().set(false);

    return searchModel;
  }

  @Override
  public final EntitySearchModel foreignKeySearchModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyProperty(foreignKey);
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
    entityDefinition().property(attribute);
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
  public final <T> void removeEditListener(Attribute<T> attribute, EventDataListener<T> listener) {
    if (valueEditEvents.containsKey(attribute)) {
      ((Event<T>) valueEditEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addEditListener(Attribute<T> attribute, EventDataListener<T> listener) {
    editEvent(attribute).addDataListener(listener);
  }

  @Override
  public final <T> void removeValueListener(Attribute<T> attribute, EventDataListener<T> listener) {
    if (valueChangeEvents.containsKey(attribute)) {
      ((Event<T>) valueChangeEvents.get(attribute)).removeDataListener(listener);
    }
  }

  @Override
  public final <T> void addValueListener(Attribute<T> attribute, EventDataListener<T> listener) {
    valueEvent(attribute).addDataListener(listener);
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
  public final void removeEntityListener(EventDataListener<Entity> listener) {
    entityEvent.removeDataListener(listener);
  }

  @Override
  public final void addEntityListener(EventDataListener<Entity> listener) {
    entityEvent.addDataListener(listener);
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
  protected List<Entity> modifiedEntities(List<Entity> entities) {
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
    notifyBeforeInsert(unmodifiableList(entities));
    validate(entities);
    //entity.toString() could potentially cause NullPointerException if null-validation
    //has not been performed, hence why this logging is performed after validation
    LOG.debug("{} - insert {}", this, entities);

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
    Map<Attribute<?>, Object> affectedAttributes = this.entity.setAs(entity == null ? defaultEntity(this::defaultValue) : entity);
    for (Map.Entry<Attribute<?>, Object> entry : affectedAttributes.entrySet()) {
      Attribute<Object> objectAttribute = (Attribute<Object>) entry.getKey();
      onValueChange(objectAttribute, this.entity.get(objectAttribute));
    }
    if (affectedAttributes.isEmpty()) {//no value changes to trigger state updates
      updateEntityStates();
    }
    updateModifiedAttributeStates();

    entityEvent.onEvent(entity);
  }

  private <T> Event<T> editEvent(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    return (Event<T>) valueEditEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private <T> Event<T> valueEvent(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    return (Event<T>) valueChangeEvents.computeIfAbsent(attribute, k -> Event.event());
  }

  private void configurePersistentForeignKeyValues() {
    if (EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get()) {
      entityDefinition().foreignKeys().forEach(foreignKey -> setPersistValue(foreignKey, isForeignKeyWritable(foreignKey)));
    }
  }

  private boolean isForeignKeyWritable(ForeignKey foreignKey) {
    return foreignKey.references().stream()
            .map(ForeignKey.Reference::attribute)
            .map(entityDefinition()::property)
            .filter(ColumnProperty.class::isInstance)
            .map(ColumnProperty.class::cast)
            .anyMatch(columnProperty -> !columnProperty.isReadOnly());
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
  private Entity defaultEntity(ValueSupplier valueSupplier) {
    EntityDefinition definition = entityDefinition();
    Entity newEntity = definition.entity();
    for (@SuppressWarnings("rawtypes") ColumnProperty property : definition.columnProperties()) {
      if (!definition.isForeignKeyAttribute(property.attribute())//these are set via their respective parent properties
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

  private <T> T defaultValue(Property<T> property) {
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

  private Map<Attribute<?>, Object> dependentValues(Attribute<?> attribute) {
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
    editEvent(attribute).onEvent(value);
    dependentValues.forEach((dependentAttribute, previousValue) -> {
      Object currentValue = get(dependentAttribute);
      if (!Objects.equals(previousValue, currentValue)) {
        notifyValueEdit((Attribute<Object>) dependentAttribute, currentValue, emptyMap());
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
      changeEvent.onEvent(value);
    }
    valueChangeEvent.onEvent(attribute);
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