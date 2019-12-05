/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.TextUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueCollectionProvider;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.model.valuemap.DefaultValueMapEditModel;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.Value;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
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

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.entityCondition;

/**
 * A default {@link EntityEditModel} implementation
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeId);
 *
 * EntityEditModel editModel = new DefaultEntityEditModel(entityId, connectionProvider);
 *
 * EntityEditPanel panel = new EntityEditPanel(editModel);
 * panel.initializePanel();
 * </pre>
 */
public abstract class DefaultEntityEditModel extends DefaultValueMapEditModel<Property, Object> implements EntityEditModel {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private static final String ENTITIES = "entities";

  private final Event<InsertEvent> beforeInsertEvent = Events.event();
  private final Event<InsertEvent> afterInsertEvent = Events.event();
  private final Event<UpdateEvent> beforeUpdateEvent = Events.event();
  private final Event<UpdateEvent> afterUpdateEvent = Events.event();
  private final Event<DeleteEvent> beforeDeleteEvent = Events.event();
  private final Event<DeleteEvent> afterDeleteEvent = Events.event();
  private final Event entitiesChangedEvent = Events.event();
  private final Event beforeRefreshEvent = Events.event();
  private final Event afterRefreshEvent = Events.event();
  private final Event<State> confirmSetEntityEvent = Events.event();

  private final State primaryKeyNullState = States.state(true);
  private final State allowInsertState = States.state(true);
  private final State allowUpdateState = States.state(true);
  private final State allowDeleteState = States.state(true);
  private final State readOnlyState = States.aggregateState(Conjunction.AND,
          allowInsertState.getReversedObserver(), allowUpdateState.getReversedObserver(), allowDeleteState.getReversedObserver());

  /**
   * The ID of the entity this edit model is based on
   */
  private final String entityId;

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
   * @see #setEntity(org.jminor.framework.domain.Entity)
   */
  private final Event<Entity> entitySetEvent = Events.event();

  /**
   * A state indicating whether or not the entity being edited is new
   * @see #isEntityNew()
   */
  private final State entityNewState = States.state(true);

  /**
   * Provides the values when a default entity is created
   */
  private final ValueProvider<Property, Object> defaultValueProvider = this::getDefaultValue;

  /**
   * Specifies whether this edit model should warn about unsaved data
   */
  private boolean warnAboutUnsavedData = WARN_ABOUT_UNSAVED_DATA.get();

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
  public DefaultEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider, final Entity.Validator validator) {
    super(connectionProvider.getDomain().entity(entityId), validator);
    this.entityId = entityId;
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    setReadOnly(getEntityDefinition().isReadOnly());
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public final Domain getDomain() {
    return connectionProvider.getDomain();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityDefinition getEntityDefinition() {
    return getDomain().getDefinition(entityId);
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().toString() + ", " + entityId;
  }

  /** {@inheritDoc} */
  @Override
  public Object getDefaultValue(final Property property) {
    if (isValuePersistent(property)) {
      final Entity entity = (Entity) getValueMap();
      if (property instanceof ForeignKeyProperty) {
        return entity.getForeignKey((ForeignKeyProperty) property);
      }

      return entity.get(property);
    }

    return property.getDefaultValue();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return readOnlyState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    allowInsertState.set(!readOnly);
    allowUpdateState.set(!readOnly);
    allowDeleteState.set(!readOnly);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setWarnAboutUnsavedData(final boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLookupAllowed(final Property property) {
    return property instanceof ColumnProperty;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValuePersistent(final Property property) {
    if (persistentValues.containsKey(property.getPropertyId())) {
      return persistentValues.get(property.getPropertyId());
    }

    return property instanceof ForeignKeyProperty && EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setValuePersistent(final String propertyId, final boolean persistValue) {
    persistentValues.put(propertyId, persistValue);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isInsertAllowed() {
    return allowInsertState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setInsertAllowed(final boolean value) {
    allowInsertState.set(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowInsertObserver() {
    return allowInsertState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public EventObserver<ValueChange<Property, Object>> getValueObserver(final String propertyId) {
    return getValueObserver(getEntityDefinition().getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return allowUpdateState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setUpdateAllowed(final boolean value) {
    allowUpdateState.set(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowUpdateObserver() {
    return allowUpdateState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteAllowed() {
    return allowDeleteState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setDeleteAllowed(final boolean value) {
    allowDeleteState.set(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowDeleteObserver() {
    return allowDeleteState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getEntityNewObserver() {
    return entityNewState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getPrimaryKeyNullObserver() {
    return primaryKeyNullState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEntity(final Entity entity) {
    if (isSetEntityAllowed()) {
      doSetEntity(entity);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {
    final List<ForeignKeyProperty> foreignKeyProperties = getEntityDefinition()
            .getForeignKeyReferences(foreignKeyEntityId);
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final Entity currentForeignKeyValue = getForeignKey(foreignKeyProperty.getPropertyId());
      if (currentForeignKeyValue != null) {
        for (final Entity newForeignKeyValue : foreignKeyValues) {
          if (currentForeignKeyValue.equals(newForeignKeyValue)) {
            put(foreignKeyProperty, null);
            put(foreignKeyProperty, newForeignKeyValue);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = getDomain().deepCopyEntity(getEntity());
    if (!includePrimaryKeyValues) {
      copy.clearKeyValues();
    }

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getForeignKey(final String foreignKeyPropertyId) {
    return (Entity) get(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getModifiedObserver() {
    return getEntity().getModifiedObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified() {
    return getModifiedObserver().get();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEntityNew() {
    return Entities.isEntityNew(getEntity());
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> entitiesByEntityId = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entityIdEntities : entitiesByEntityId.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition()
              .getForeignKeyReferences(entityIdEntities.getKey())) {
        //todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
        put(foreignKeyProperty, entityIdEntities.getValue().iterator().next());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Object get(final String propertyId) {
    return get(getEntityDefinition().getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final void put(final String propertyId, final Object value) {
    put(getEntityDefinition().getProperty(propertyId), value);
  }

  /** {@inheritDoc} */
  @Override
  public final Object remove(final String propertyId) {
    return remove(getEntityDefinition().getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNull(final String propertyId) {
    return isNull(getEntityDefinition().getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNotNull(final String propertyId) {
    return !isNull(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !getEntityDefinition().isKeyGenerated();
    final Entity toInsert = getEntityCopy(includePrimaryKeyValues);
    toInsert.saveAll();
    final List<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    doSetEntity(insertedEntities.get(0));

    fireAfterInsertEvent(new DefaultInsertEvent(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    final List<Entity> insertedEntities = insertEntities(entities);

    fireAfterInsertEvent(new DefaultInsertEvent(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update() throws DatabaseException, ValidationException {
    return update(singletonList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    if (!isUpdateAllowed()) {
      throw new IllegalStateException("This model does not allow updating!");
    }

    LOG.debug("{} - update {}", this, TextUtil.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    fireBeforeUpdateEvent(new DefaultUpdateEvent(Entities.mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(entities))));
    validate(modifiedEntities);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    fireAfterUpdateEvent(new DefaultUpdateEvent(Entities.mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(updatedEntities))));

    return updatedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete() throws DatabaseException {
    final Entity originalEntity = getEntityCopy();
    originalEntity.revertAll();

    return delete(singletonList(originalEntity));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    if (!isDeleteAllowed()) {
      throw new IllegalStateException("This model does not allow deleting!");
    }

    LOG.debug("{} - delete {}", this, TextUtil.getCollectionContentsAsString(entities, false));

    fireBeforeDeleteEvent(new DefaultDeleteEvent(entities));

    final List<Entity> deleted = doDelete(entities);
    if (deleted.contains(getEntity())) {
      doSetEntity(null);
    }

    fireAfterDeleteEvent(new DefaultDeleteEvent(deleted));

    return deleted;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    try {
      beforeRefreshEvent.fire();
      refreshDataModels();
    }
    finally {
      afterRefreshEvent.fire();
    }
  }

  protected void refreshDataModels() {}

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final boolean containsLookupModel(final String foreignKeyPropertyId) {
    return entityLookupModels.containsKey(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel createForeignKeyLookupModel(final ForeignKeyProperty foreignKeyProperty) {
    final Collection<ColumnProperty> searchProperties = getDomain()
            .getDefinition(foreignKeyProperty.getForeignEntityId()).getSearchProperties();
    if (searchProperties.isEmpty()) {
      throw new IllegalStateException("No search properties defined for entity: " + foreignKeyProperty.getForeignEntityId());
    }

    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(foreignKeyProperty.getForeignEntityId(), connectionProvider, searchProperties);
    lookupModel.getMultipleSelectionAllowedValue().set(false);

    return lookupModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getForeignKeyLookupModel(final String foreignKeyPropertyId) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    return getForeignKeyLookupModel(getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getForeignKeyLookupModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    return entityLookupModels.computeIfAbsent(foreignKeyProperty, fk -> createForeignKeyLookupModel(foreignKeyProperty));
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getDefaultEntity() {
    return getDomain().defaultEntity(entityId, defaultValueProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(connectionProvider, entityId, property.getPropertyId());
  }

  /** {@inheritDoc} */
  @Override
  public final <V> Value<V> value(final String propertyId) {
    return new EditModelValue<>(this, propertyId);
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final void removeValueSetListener(final String propertyId, final EventDataListener listener) {
    removeValueSetListener(getEntityDefinition().getProperty(propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueSetListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    addValueSetListener(getEntityDefinition().getProperty(propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final String propertyId, final EventDataListener listener) {
    removeValueListener(getEntityDefinition().getProperty(propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    addValueListener(getEntityDefinition().getProperty(propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEntitySetListener(final EventDataListener listener) {
    entitySetEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitySetListener(final EventDataListener<Entity> listener) {
    entitySetEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeInsertListener(final EventDataListener listener) {
    beforeInsertEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeInsertListener(final EventDataListener<InsertEvent> listener) {
    beforeInsertEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterInsertListener(final EventDataListener listener) {
    afterInsertEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterInsertListener(final EventDataListener<InsertEvent> listener) {
    afterInsertEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeUpdateListener(final EventDataListener listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeUpdateListener(final EventDataListener<UpdateEvent> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterUpdateListener(final EventDataListener listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterUpdateListener(final EventDataListener<UpdateEvent> listener) {
    afterUpdateEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeDeleteListener(final EventDataListener<DeleteEvent> listener) {
    beforeDeleteEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeDeleteListener(final EventDataListener listener) {
    beforeDeleteEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterDeleteListener(final EventDataListener listener) {
    afterDeleteEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterDeleteListener(final EventDataListener<DeleteEvent> listener) {
    afterDeleteEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEntitiesChangedListener(final EventListener listener) {
    entitiesChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitiesChangedListener(final EventListener listener) {
    entitiesChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeRefreshListener(final EventListener listener) {
    beforeRefreshEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    beforeRefreshEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addConfirmSetEntityObserver(final EventDataListener<State> listener) {
    confirmSetEntityEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeConfirmSetEntityObserver(final EventDataListener listener) {
    confirmSetEntityEvent.removeDataListener(listener);
  }

  /**
   * @return the actual {@link Entity} instance being edited
   */
  protected final Entity getEntity() {
    return (Entity) getValueMap();
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

  /**
   * Notifies that a insert is about to be performed
   * @param insertEvent the event describing the insert
   * @see #addBeforeInsertListener(EventDataListener)
   */
  protected final void fireBeforeInsertEvent(final InsertEvent insertEvent) {
    beforeInsertEvent.fire(insertEvent);
  }

  /**
   * Notifies that a insert has been performed
   * @param insertEvent the event describing the insert
   * @see #addAfterInsertListener(EventDataListener)
   */
  protected final void fireAfterInsertEvent(final InsertEvent insertEvent) {
    afterInsertEvent.fire(insertEvent);
    EntityEditEvents.inserted(insertEvent);
  }

  /**
   * Notifies that an update is about to be performed
   * @param updateEvent the event describing the update
   * @see #addBeforeUpdateListener(EventDataListener)
   */
  protected final void fireBeforeUpdateEvent(final UpdateEvent updateEvent) {
    beforeUpdateEvent.fire(updateEvent);
  }

  /**
   * Notifies that an update has been performed
   * @param updateEvent the event describing the update
   * @see #addAfterUpdateListener(EventDataListener)
   */
  protected final void fireAfterUpdateEvent(final UpdateEvent updateEvent) {
    afterUpdateEvent.fire(updateEvent);
    EntityEditEvents.updated(updateEvent);
  }

  /**
   * Notifies that a delete is about to be performed
   * @param deleteEvent the event describing the delete
   * @see #addBeforeDeleteListener(EventDataListener)
   */
  protected final void fireBeforeDeleteEvent(final DeleteEvent deleteEvent) {
    beforeDeleteEvent.fire(deleteEvent);
  }

  /**
   * Notifies that a delete has been performed
   * @param deleteEvent the event describing the delete
   * @see #addAfterDeleteListener(EventDataListener)
   */
  protected final void fireAfterDeleteEvent(final DeleteEvent deleteEvent) {
    afterDeleteEvent.fire(deleteEvent);
    EntityEditEvents.deleted(deleteEvent);
  }

  private List<Entity> insertEntities(final List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertAllowed()) {
      throw new IllegalStateException("This model does not allow inserting!");
    }

    LOG.debug("{} - insert {}", this, TextUtil.getCollectionContentsAsString(entities, false));

    fireBeforeInsertEvent(new DefaultInsertEvent(entities));
    validate(entities);

    return connectionProvider.getConnection().select(doInsert(entities));
  }

  private boolean isSetEntityAllowed() {
    if (warnAboutUnsavedData && containsUnsavedData()) {
      final State confirmation = States.state(true);
      confirmSetEntityEvent.fire(confirmation);

      return confirmation.get();
    }

    return true;
  }

  private void doSetEntity(final Entity entity) {
    getEntity().setAs(entity == null ? getDefaultEntity() : entity);
    entitySetEvent.fire(entity);
  }

  private boolean valueModified(final Property property) {
    return !Objects.equals(get(property), getDefaultValue(property));
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesChangedEvent);
    afterInsertEvent.addListener(entitiesChangedEvent);
    afterUpdateEvent.addListener(entitiesChangedEvent);
    getEntity().addValueListener(valueChange -> {
      primaryKeyNullState.set(getEntity().isKeyNull());
      entityNewState.set(isEntityNew());
    });
  }

  static final class PropertyValueProvider implements ValueCollectionProvider<Object> {

    private final EntityConnectionProvider connectionProvider;
    private final String entityId;
    private final String propertyId;

    private PropertyValueProvider(final EntityConnectionProvider connectionProvider, final String entityId,
                                  final String propertyId) {
      this.connectionProvider = connectionProvider;
      this.entityId = entityId;
      this.propertyId = propertyId;
    }

    @Override
    public Collection<Object> values() {
      try {
        return connectionProvider.getConnection().selectValues(propertyId, entityCondition(entityId));
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class EditModelValue<V> extends AbstractValue<V> {

    private final EntityEditModel editModel;
    private final String propertyId;

    private EditModelValue(final EntityEditModel editModel, final String propertyId) {
      this.editModel = editModel;
      this.propertyId = propertyId;
      this.editModel.getValueObserver(propertyId).addDataListener(valueChange -> fireChangeEvent(get()));
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

  protected static final class DefaultInsertEvent implements InsertEvent {

    private final List<Entity> insertedEntities;

    /**
     * Instantiates a new DefaultInsertEvent.
     * @param insertedEntities the inserted entities
     */
    public DefaultInsertEvent(final List<Entity> insertedEntities) {
      this.insertedEntities = unmodifiableList(insertedEntities);
    }

    @Override
    public List<Entity> getInsertedEntities() {
      return insertedEntities;
    }
  }

  protected static final class DefaultDeleteEvent implements DeleteEvent {

    private final List<Entity> deletedEntities;

    /**
     * Instantiates a new DefaultDeleteEvent.
     * @param deletedEntities the deleted entities
     */
    public DefaultDeleteEvent(final List<Entity> deletedEntities) {
      this.deletedEntities = unmodifiableList(deletedEntities);
    }

    @Override
    public List<Entity> getDeletedEntities() {
      return deletedEntities;
    }
  }

  protected static final class DefaultUpdateEvent implements UpdateEvent {

    private final Map<Entity.Key, Entity> updatedEntities;

    /**
     * Instantiates a new DefaultUpdateEvent.
     * @param updatedEntities the updated entities, mapped to their respective original primary key, that is,
     * the primary key prior to the update
     */
    public DefaultUpdateEvent(final Map<Entity.Key, Entity> updatedEntities) {
      this.updatedEntities = unmodifiableMap(updatedEntities);
    }

    @Override
    public Map<Entity.Key, Entity> getUpdatedEntities() {
      return updatedEntities;
    }
  }
}