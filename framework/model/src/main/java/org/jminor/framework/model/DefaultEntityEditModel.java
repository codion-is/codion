/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.UpdateException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.entity.Validator;
import org.jminor.framework.domain.entity.ValueChange;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.jminor.framework.domain.entity.Entities.mapToOriginalPrimaryKey;
import static org.jminor.framework.domain.entity.ValueChanges.valueChange;

/**
 * A default {@link EntityEditModel} implementation
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = Users.user("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeId);
 *
 * EntityEditModel editModel = new DefaultEntityEditModel(entityId, connectionProvider);
 *
 * EntityEditPanel panel = new EntityEditPanel(editModel);
 * panel.initializePanel();
 * </pre>
 */
public abstract class DefaultEntityEditModel implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private static final String ENTITIES = "entities";
  private static final String PROPERTY = "property";

  private final Event<List<Entity>> beforeInsertEvent = Events.event();
  private final Event<List<Entity>> afterInsertEvent = Events.event();
  private final Event<Map<Entity.Key, Entity>> beforeUpdateEvent = Events.event();
  private final Event<Map<Entity.Key, Entity>> afterUpdateEvent = Events.event();
  private final Event<List<Entity>> beforeDeleteEvent = Events.event();
  private final Event<List<Entity>> afterDeleteEvent = Events.event();
  private final Event entitiesChangedEvent = Events.event();
  private final Event beforeRefreshEvent = Events.event();
  private final Event afterRefreshEvent = Events.event();
  private final Event<State> confirmSetEntityEvent = Events.event();

  private final State entityModifiedState = States.state();
  private final State primaryKeyNullState = States.state(true);
  private final State insertEnabledState = States.state(true);
  private final State updateEnabledState = States.state(true);
  private final State deleteEnabledState = States.state(true);
  private final State readOnlyState = States.aggregateState(Conjunction.AND,
          insertEnabledState.getReversedObserver(), updateEnabledState.getReversedObserver(), deleteEnabledState.getReversedObserver());

  /**
   * The Entity being edited by this model
   */
  private final Entity entity;

  /**
   * The {@link EntityConnectionProvider} instance to use
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the EntityLookupModels used by this {@link EntityEditModel}
   */
  private final Map<ForeignKeyProperty, EntityLookupModel> entityLookupModels = new HashMap<>();

  /**
   * Contains true if values should persist for the given property when the model is cleared
   */
  private final Map<String, Boolean> persistentValues = new HashMap<>();

  /**
   * Fired when the active entity is set.
   * @see #setEntity(Entity)
   */
  private final Event<Entity> entitySetEvent = Events.event();

  /**
   * The validator used by this edit model
   */
  private final Validator validator;

  /**
   * A state indicating whether the entity being edited is in a valid state according the validator
   */
  private final State validState = States.state();

  /**
   * Holds events signaling value changes made via {@link #put(Property, Object)} or {@link #remove(Property)}
   */
  private final Map<String, Event<ValueChange>> valueEditEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes in the underlying {@link Entity}
   */
  private final Map<String, Event<ValueChange>> valueChangeEventMap = new HashMap<>();

  /**
   * A state indicating whether the entity being edited is new
   * @see #isEntityNew()
   */
  private final State entityNewState = States.state(true);

  /**
   * Provides the default value for properties when a default entity is created
   */
  private final Function<Property, Object> defaultValueProvider = this::getDefaultValue;

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
   * Instantiates a new {@link DefaultEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public DefaultEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, connectionProvider.getDomain().getDefinition(entityId).getValidator());
  }

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the entity identified by {@code entityId}.
   * @param entityId the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider,
                                final Validator validator) {
    this.entity = connectionProvider.getDomain().entity(entityId);
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.validator = validator;
    setReadOnly(getEntityDefinition().isReadOnly());
    initializePersistentValues();
    bindEventsInternal();
  }

  @Override
  public final Domain getDomain() {
    return connectionProvider.getDomain();
  }

  @Override
  public final EntityDefinition getEntityDefinition() {
    return getDomain().getDefinition(entity.getEntityId());
  }

  @Override
  public final String toString() {
    return getClass().toString() + ", " + entity.getEntityId();
  }

  @Override
  public Object getDefaultValue(final Property property) {
    if (isPersistValue(property)) {
      if (property instanceof ForeignKeyProperty) {
        return entity.getForeignKey((ForeignKeyProperty) property);
      }

      return entity.get(property);
    }

    return property.getDefaultValue();
  }

  @Override
  public final boolean isReadOnly() {
    return readOnlyState.get();
  }

  @Override
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    insertEnabledState.set(!readOnly);
    updateEnabledState.set(!readOnly);
    deleteEnabledState.set(!readOnly);
    return this;
  }

  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  @Override
  public final EntityEditModel setWarnAboutUnsavedData(final boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
    return this;
  }

  @Override
  public boolean isPersistValue(final Property property) {
    if (persistentValues.containsKey(property.getPropertyId())) {
      return persistentValues.get(property.getPropertyId());
    }

    return false;
  }

  @Override
  public final EntityEditModel setPersistValue(final String propertyId, final boolean persistValue) {
    persistentValues.put(propertyId, persistValue);
    return this;
  }

  @Override
  public final boolean isInsertEnabled() {
    return insertEnabledState.get();
  }

  @Override
  public final EntityEditModel setInsertEnabled(final boolean insertEnabled) {
    insertEnabledState.set(insertEnabled);
    return this;
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
  public final EntityEditModel setUpdateEnabled(final boolean updateEnabled) {
    updateEnabledState.set(updateEnabled);
    return this;
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
  public final EntityEditModel setDeleteEnabled(final boolean deleteEnabled) {
    deleteEnabledState.set(deleteEnabled);
    return this;
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
  public final String getEntityId() {
    return entity.getEntityId();
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final void replaceForeignKeyValues(final Collection<Entity> entities) {
    final Map<String, List<Entity>> entitiesByEntityId = Entities.mapToEntityId(entities);
    for (final Map.Entry<String, List<Entity>> entityIdEntities : entitiesByEntityId.entrySet()) {
      final List<ForeignKeyProperty> foreignKeyProperties = getEntityDefinition()
              .getForeignKeyReferences(entityIdEntities.getKey());
      for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        replaceForeignKey(foreignKeyProperty, entityIdEntities.getValue());
      }
    }
  }

  @Override
  public final Entity getEntityCopy() {
    return getDomain().deepCopyEntity(getEntity());
  }

  @Override
  public final Entity getForeignKey(final String foreignKeyPropertyId) {
    return (Entity) get(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  @Override
  public StateObserver getModifiedObserver() {
    return entityModifiedState.getObserver();
  }

  @Override
  public final boolean isModified() {
    return entity.isModified();
  }

  @Override
  public boolean isEntityNew() {
    return Entities.isEntityNew(getEntity());
  }

  @Override
  public final void setForeignKeyValues(final Collection<Entity> entities) {
    final Map<String, List<Entity>> entitiesByEntityId = Entities.mapToEntityId(entities);
    for (final Map.Entry<String, List<Entity>> entityIdEntities : entitiesByEntityId.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition()
              .getForeignKeyReferences(entityIdEntities.getKey())) {
        //todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
        put(foreignKeyProperty, entityIdEntities.getValue().iterator().next());
      }
    }
  }

  @Override
  public final Object get(final String propertyId) {
    return entity.get(propertyId);
  }

  @Override
  public final void put(final String propertyId, final Object value) {
    put(getEntityDefinition().getProperty(propertyId), value);
  }

  @Override
  public final Object remove(final String propertyId) {
    return remove(getEntityDefinition().getProperty(propertyId));
  }

  @Override
  public final Object get(final Property property) {
    return entity.get(property);
  }

  @Override
  public final void put(final Property property, final Object value) {
    requireNonNull(property, PROPERTY);
    final boolean initialization = !entity.containsKey(property);
    final Object previousValue = entity.put(property, value);
    if (!Objects.equals(value, previousValue)) {
      getValueEditEvent(property.getPropertyId()).onEvent(valueChange(property, value, previousValue, initialization));
    }
  }

  @Override
  public final Object remove(final Property property) {
    requireNonNull(property, PROPERTY);
    Object value = null;
    if (entity.containsKey(property)) {
      value = entity.remove(property);
      getValueEditEvent(property.getPropertyId()).onEvent(valueChange(property, null, value));
    }

    return value;
  }

  @Override
  public final boolean isNullable(final Property property) {
    return validator.isNullable(entity, property);
  }

  @Override
  public final boolean isNull(final String propertyId) {
    return entity.isNull(propertyId);
  }

  @Override
  public final boolean isNotNull(final String propertyId) {
    return !entity.isNull(propertyId);
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
  public final void validate(final Property property) throws ValidationException {
    validator.validate(entity, getEntityDefinition(), property);
  }

  @Override
  public final void validate() throws ValidationException {
    validate(entity);
  }

  @Override
  public final void validate(final Entity entity) throws ValidationException {
    validate(singletonList(entity));
  }

  @Override
  public final void validate(final Collection<Entity> entities) throws ValidationException {
    for (final Entity entityToValidate : entities) {
      validator.validate(entityToValidate, getDomain().getDefinition(entityToValidate.getEntityId()));
    }
  }

  @Override
  public final boolean isValid(final Property property) {
    try {
      validator.validate(entity, getEntityDefinition(), requireNonNull(property, PROPERTY));
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  @Override
  public final Validator getValidator() {
    return validator;
  }

  @Override
  public final Entity insert() throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("Inserting is not enabled!");
    }
    final Entity toInsert = getEntityCopy();
    if (getEntityDefinition().isKeyGenerated()) {
      toInsert.clearKeyValues();
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
    LOG.debug("{} - update {}", this, entities.toString());

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
    LOG.debug("{} - delete {}", this, entities.toString());

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
        setEntity(getConnectionProvider().getConnection().selectSingle(getEntity().getKey()));
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean containsLookupModel(final String foreignKeyPropertyId) {
    return entityLookupModels.containsKey(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  @Override
  public EntityLookupModel createForeignKeyLookupModel(final ForeignKeyProperty foreignKeyProperty) {
    final Collection<ColumnProperty> searchProperties = getDomain()
            .getDefinition(foreignKeyProperty.getForeignEntityId()).getSearchProperties();
    if (searchProperties.isEmpty()) {
      throw new IllegalStateException("No search properties defined for entity: " + foreignKeyProperty.getForeignEntityId());
    }

    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(foreignKeyProperty.getForeignEntityId(), connectionProvider, searchProperties);
    lookupModel.getMultipleSelectionEnabledValue().set(false);

    return lookupModel;
  }

  @Override
  public final EntityLookupModel getForeignKeyLookupModel(final String foreignKeyPropertyId) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    return getForeignKeyLookupModel(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  @Override
  public final EntityLookupModel getForeignKeyLookupModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    return entityLookupModels.computeIfAbsent(foreignKeyProperty, fk -> createForeignKeyLookupModel(foreignKeyProperty));
  }

  @Override
  public final Entity getDefaultEntity() {
    return getDomain().defaultEntity(entity.getEntityId(), defaultValueProvider);
  }

  @Override
  public final <V> Value<V> value(final String propertyId) {
    return new EditModelValue<>(this, propertyId);
  }

  @Override
  public final boolean containsUnsavedData() {
    if (isEntityNew()) {
      final EntityDefinition entityDefinition = getEntityDefinition();
      for (final ColumnProperty property : entityDefinition.getColumnProperties()) {
        if (!property.isForeignKeyProperty() && valueModified(property)) {
          return true;
        }
      }
      for (final ForeignKeyProperty property : entityDefinition.getForeignKeyProperties()) {
        if (valueModified(property)) {
          return true;
        }
      }

      return false;
    }

    return !getEntity().originalKeySet().isEmpty();
  }

  @Override
  public final boolean isPostEditEvents() {
    return postEditEvents;
  }

  @Override
  public final EntityEditModel setPostEditEvents(final boolean postEditEvents) {
    this.postEditEvents = postEditEvents;
    return this;
  }

  @Override
  public final void removeValueEditListener(final String propertyId, final EventDataListener<ValueChange> listener) {
    if (valueEditEventMap.containsKey(propertyId)) {
      valueEditEventMap.get(propertyId).removeDataListener(listener);
    }
  }

  @Override
  public final void addValueEditListener(final String propertyId, final EventDataListener<ValueChange> listener) {
    getValueEditEvent(propertyId).addDataListener(listener);
  }

  @Override
  public final void removeValueListener(final String propertyId, final EventDataListener<ValueChange> listener) {
    if (valueChangeEventMap.containsKey(propertyId)) {
      valueChangeEventMap.get(propertyId).removeDataListener(listener);
    }
  }

  @Override
  public final void addValueListener(final String propertyId, final EventDataListener<ValueChange> listener) {
    getValueChangeEvent(propertyId).addDataListener(listener);
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
  public final void removeBeforeUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  @Override
  public final void removeAfterUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  @Override
  public final void addAfterUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
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
  public final void removeEntitiesChangedListener(final EventListener listener) {
    entitiesChangedEvent.removeListener(listener);
  }

  @Override
  public final void addEntitiesChangedListener(final EventListener listener) {
    entitiesChangedEvent.addListener(listener);
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
  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DatabaseException {
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
    connectionProvider.getConnection().delete(Entities.getKeys(entities));

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
    return Entities.getModifiedEntities(entities);
  }

  protected void refreshDataModels() {}

  /**
   * For every field referencing the given foreign key values, replaces that foreign key instance with
   * the corresponding entity from {@code values}, useful when property
   * values have been changed in the referenced entity that must be reflected in the edit model.
   * @param foreignKeyProperty the foreign key property
   * @param values the foreign key entities
   */
  protected void replaceForeignKey(final ForeignKeyProperty foreignKeyProperty, final List<Entity> values) {
    final Entity currentForeignKeyValue = getForeignKey(foreignKeyProperty.getPropertyId());
    if (currentForeignKeyValue != null) {
      for (final Entity replacementValue : values) {
        if (currentForeignKeyValue.equals(replacementValue)) {
          put(foreignKeyProperty, null);
          put(foreignKeyProperty, replacementValue);
        }
      }
    }
  }

  /**
   * Notifies that a insert is about to be performed
   * @param entitiesToInsert the entities about to be inserted
   * @see #addBeforeInsertListener(EventDataListener)
   */
  protected final void notifyBeforeInsert(final List<Entity> entitiesToInsert) {
    beforeInsertEvent.onEvent(entitiesToInsert);
  }

  /**
   * Notifies that a insert has been performed
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
   * Notifies that an update is about to be performed
   * @param entitiesToUpdate the entities about to be updated
   * @see #addBeforeUpdateListener(EventDataListener)
   */
  protected final void notifyBeforeUpdate(final Map<Entity.Key, Entity> entitiesToUpdate) {
    beforeUpdateEvent.onEvent(entitiesToUpdate);
  }

  /**
   * Notifies that an update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(EventDataListener)
   */
  protected final void notifyAfterUpdate(final Map<Entity.Key, Entity> updatedEntities) {
    afterUpdateEvent.onEvent(updatedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyUpdated(updatedEntities);
    }
  }

  /**
   * Notifies that a delete is about to be performed
   * @param entitiesToDelete the entities about to be deleted
   * @see #addBeforeDeleteListener(EventDataListener)
   */
  protected final void notifyBeforeDelete(final List<Entity> entitiesToDelete) {
    beforeDeleteEvent.onEvent(entitiesToDelete);
  }

  /**
   * Notifies that a delete has been performed
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
      final State confirmation = States.state(true);
      confirmSetEntityEvent.onEvent(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void doSetEntity(final Entity entity) {
    getEntity().setAs(entity == null ? getDefaultEntity() : entity);
    entitySetEvent.onEvent(entity);
  }

  private boolean valueModified(final Property property) {
    return !Objects.equals(get(property), getDefaultValue(property));
  }

  private Event<ValueChange> getValueEditEvent(final String propertyId) {
    return valueEditEventMap.computeIfAbsent(propertyId, k -> Events.event());
  }

  private Event<ValueChange> getValueChangeEvent(final String propertyId) {
    return valueChangeEventMap.computeIfAbsent(propertyId, k -> Events.event());
  }

  private void initializePersistentValues() {
    if (EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get()) {
      getEntityDefinition().getForeignKeyProperties().forEach(property -> setPersistValue(property.getPropertyId(), true));
    }
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesChangedEvent);
    afterInsertEvent.addListener(entitiesChangedEvent);
    afterUpdateEvent.addListener(entitiesChangedEvent);
    entity.addValueListener(valueChange -> {
      entityModifiedState.set(entity.isModified());
      validState.set(validator.isValid(entity, getEntityDefinition()));
      primaryKeyNullState.set(entity.getKey().isNull());
      entityNewState.set(isEntityNew());
      final Event<ValueChange> valueChangeEvent = valueChangeEventMap.get(valueChange.getProperty().getPropertyId());
      if (valueChangeEvent != null) {
        valueChangeEvent.onEvent(valueChange);
      }
    });
  }

  private static final class EditModelValue<V> extends AbstractValue<V> {

    private final EntityEditModel editModel;
    private final String propertyId;

    private EditModelValue(final EntityEditModel editModel, final String propertyId) {
      this.editModel = editModel;
      this.propertyId = propertyId;
      this.editModel.addValueListener(propertyId, valueChange -> notifyValueChange());
    }

    @Override
    public V get() {
      return (V) editModel.get(propertyId);
    }

    @Override
    public void set(final V value) {
      editModel.put(propertyId, value);
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }
}