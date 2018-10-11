/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.TextUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueCollectionProvider;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.model.valuemap.DefaultValueMapEditModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
  private final Map<Property.ForeignKeyProperty, EntityLookupModel> entityLookupModels = new HashMap<>();

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
    this(entityId, connectionProvider, connectionProvider.getDomain().getValidator(entityId));
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
    this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    setReadOnly(connectionProvider.getDomain().isReadOnly(entityId));
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public Entities getDomain() {
    return connectionProvider.getDomain();
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
      if (property instanceof Property.ForeignKeyProperty) {
        return entity.getForeignKey((Property.ForeignKeyProperty) property);
      }

      return entity.get(property);
    }

    return property.getDefaultValue();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return readOnlyState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    allowInsertState.setActive(!readOnly);
    allowUpdateState.setActive(!readOnly);
    allowDeleteState.setActive(!readOnly);
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
    return property instanceof Property.ColumnProperty;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValuePersistent(final Property property) {
    if (persistentValues.containsKey(property.getPropertyId())) {
      return persistentValues.get(property.getPropertyId());
    }

    return property instanceof Property.ForeignKeyProperty && EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get();
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
    return allowInsertState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setInsertAllowed(final boolean value) {
    allowInsertState.setActive(value);
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
    return getValueObserver(getDomain().getProperty(entityId, propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return allowUpdateState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setUpdateAllowed(final boolean value) {
    allowUpdateState.setActive(value);
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
    return allowDeleteState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setDeleteAllowed(final boolean value) {
    allowDeleteState.setActive(value);
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
    final List<Property.ForeignKeyProperty> foreignKeyProperties = getDomain()
            .getForeignKeyProperties(this.entityId, foreignKeyEntityId);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      final Entity currentForeignKeyValue = getForeignKeyValue(foreignKeyProperty.getPropertyId());
      if (currentForeignKeyValue != null) {
        for (final Entity newForeignKeyValue : foreignKeyValues) {
          if (currentForeignKeyValue.equals(newForeignKeyValue)) {
            setValue(foreignKeyProperty, null);
            setValue(foreignKeyProperty, newForeignKeyValue);
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
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includePrimaryKeyValues) {
      copy.clearKeyValues();
    }

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getForeignKeyValue(final String foreignKeyPropertyId) {
    return (Entity) getValue(getDomain().getForeignKeyProperty(entityId, foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getModifiedObserver() {
    return getEntity().getModifiedObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified() {
    return getModifiedObserver().isActive();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEntityNew() {
    return Entities.isEntityNew(getEntity());
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyValues(final List<Entity> values) {
    final Map<String, List<Entity>> mapped = Entities.mapToEntityId(values);
    for (final Map.Entry<String, List<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : getDomain()
              .getForeignKeyProperties(entityId, entry.getKey())) {
        //todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
        setValue(foreignKeyProperty, entry.getValue().iterator().next());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public Object getValue(final String propertyId) {
    return getValue(getDomain().getProperty(entityId, propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public void setValue(final String propertyId, final Object value) {
    setValue(getDomain().getProperty(entityId, propertyId), value);
  }

  /** {@inheritDoc} */
  @Override
  public Object removeValue(final String propertyId) {
    return removeValue(getDomain().getProperty(entityId, propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValueNull(final String propertyId) {
    return isValueNull(getDomain().getProperty(entityId, propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !getDomain().isPrimaryKeyAutoGenerated(entityId);
    final Entity toInsert = getEntityCopy(includePrimaryKeyValues);
    toInsert.saveAll();
    final List<Entity> insertedEntities = insertEntities(Collections.singletonList(toInsert));
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
    Objects.requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    final List<Entity> insertedEntities = insertEntities(entities);

    fireAfterInsertEvent(new DefaultInsertEvent(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update() throws DatabaseException, ValidationException {
    return update(Collections.singletonList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException {
    Objects.requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    if (!isUpdateAllowed()) {
      throw new IllegalStateException("This model does not allow updating!");
    }

    LOG.debug("{} - update {}", this, TextUtil.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return Collections.emptyList();
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
    return delete(Collections.singletonList((Entity) getEntity().getOriginalCopy()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return Collections.emptyList();
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
    return entityLookupModels.containsKey(getDomain().getForeignKeyProperty(entityId, foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel createForeignKeyLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Collection<Property.ColumnProperty> searchProperties = getDomain()
            .getSearchProperties(foreignKeyProperty.getForeignEntityId());
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
    Objects.requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    return getForeignKeyLookupModel(getDomain().getForeignKeyProperty(entityId, foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getForeignKeyLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
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
  public final boolean containsUnsavedData() {
    if (isEntityNew()) {
      for (final Property.ColumnProperty property : getDomain().getColumnProperties(entityId)) {
        if (!property.isForeignKeyProperty() && valueModified(property)) {
          return true;
        }
      }
      for (final Property.ForeignKeyProperty property : getDomain().getForeignKeyProperties(entityId)) {
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
  public void removeValueSetListener(final String propertyId, final EventDataListener listener) {
    removeValueSetListener(getDomain().getProperty(entityId, propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addValueSetListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    addValueSetListener(getDomain().getProperty(entityId, propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeValueListener(final String propertyId, final EventDataListener listener) {
    removeValueListener(getDomain().getProperty(entityId, propertyId), listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addValueListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    addValueListener(getDomain().getProperty(entityId, propertyId), listener);
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
  }

  private List<Entity> insertEntities(final List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertAllowed()) {
      throw new IllegalStateException("This model does not allow inserting!");
    }

    LOG.debug("{} - insert {}", this, TextUtil.getCollectionContentsAsString(entities, false));

    fireBeforeInsertEvent(new DefaultInsertEvent(entities));
    validate(entities);

    return connectionProvider.getConnection().selectMany(doInsert(entities));
  }

  private boolean isSetEntityAllowed() {
    if (warnAboutUnsavedData && containsUnsavedData()) {
      final State confirmation = States.state(true);
      confirmSetEntityEvent.fire(confirmation);

      return confirmation.isActive();
    }

    return true;
  }

  private void doSetEntity(final Entity entity) {
    getEntity().setAs(entity == null ? getDefaultEntity() : entity);
    entitySetEvent.fire(entity);
  }

  private boolean valueModified(final Property property) {
    return !Objects.equals(getValue(property), getDefaultValue(property));
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesChangedEvent);
    afterInsertEvent.addListener(entitiesChangedEvent);
    afterUpdateEvent.addListener(entitiesChangedEvent);
    getEntity().addValueListener(valueChange -> {
      primaryKeyNullState.setActive(getEntity().isKeyNull());
      entityNewState.setActive(isEntityNew());
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
        return connectionProvider.getConnection().selectValues(propertyId,
                connectionProvider.getConditions().condition(entityId));
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected static final class DefaultInsertEvent implements InsertEvent {

    private final List<Entity> insertedEntities;

    /**
     * Instantiates a new DefaultInsertEvent.
     * @param insertedEntities the inserted entities
     */
    public DefaultInsertEvent(final List<Entity> insertedEntities) {
      this.insertedEntities = insertedEntities;
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
      this.deletedEntities = deletedEntities;
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
      this.updatedEntities = updatedEntities;
    }

    @Override
    public Map<Entity.Key, Entity> getUpdatedEntities() {
      return updatedEntities;
    }
  }
}