/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.UpdateException;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueCollectionProvider;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
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
import static org.jminor.common.db.valuemap.ValueChanges.valueChange;
import static org.jminor.framework.db.condition.Conditions.entityCondition;
import static org.jminor.framework.domain.Entities.mapToOriginalPrimaryKey;

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
   * @see #setEntity(org.jminor.framework.domain.Entity)
   */
  private final Event<Entity> entitySetEvent = Events.event();

  /**
   * The validator used by this edit model
   */
  private final Entity.Validator validator;

  /**
   * A state indicating whether the entity being edited is in a valid state according the validator
   */
  private final State validState = States.state();

  /**
   * Holds events signaling value changes made via {@link #put(Property, Object)} or {@link #remove(Property)}
   */
  private final Map<String, Event<ValueChange<Property, Object>>> valueEditEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes in the underlying {@link Entity}
   */
  private final Map<String, Event<ValueChange<Property, Object>>> valueChangeEventMap = new HashMap<>();

  /**
   * A state indicating whether the entity being edited is new
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
                                final Entity.Validator validator) {
    this.entity = connectionProvider.getDomain().entity(entityId);
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.validator = validator;
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
    return getDomain().getDefinition(entity.getEntityId());
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().toString() + ", " + entity.getEntityId();
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return readOnlyState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    insertEnabledState.set(!readOnly);
    updateEnabledState.set(!readOnly);
    deleteEnabledState.set(!readOnly);
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
  public boolean isLookupEnabled(final Property property) {
    return property instanceof ColumnProperty;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPersistValue(final Property property) {
    if (persistentValues.containsKey(property.getPropertyId())) {
      return persistentValues.get(property.getPropertyId());
    }

    return property instanceof ForeignKeyProperty && EntityEditModel.PERSIST_FOREIGN_KEY_VALUES.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setPersistValue(final String propertyId, final boolean persistValue) {
    persistentValues.put(propertyId, persistValue);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isInsertEnabled() {
    return insertEnabledState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setInsertEnabled(final boolean insertEnabled) {
    insertEnabledState.set(insertEnabled);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getInsertEnabledObserver() {
    return insertEnabledState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<Property, Object>> getValueObserver(final String propertyId) {
    return getValueChangeEvent(requireNonNull(propertyId, PROPERTY)).getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateEnabled() {
    return updateEnabledState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setUpdateEnabled(final boolean updateEnabled) {
    updateEnabledState.set(updateEnabled);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getUpdateEnabledObserver() {
    return updateEnabledState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteEnabled() {
    return deleteEnabledState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setDeleteEnabled(final boolean deleteEnabled) {
    deleteEnabledState.set(deleteEnabled);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getDeleteEnabledObserver() {
    return deleteEnabledState.getObserver();
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
    return entity.getEntityId();
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
    return entity.get(propertyId);
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
  public final Object get(final Property property) {
    return entity.get(property);
  }

  /** {@inheritDoc} */
  @Override
  public final void put(final Property property, final Object value) {
    requireNonNull(property, PROPERTY);
    final boolean initialization = !entity.containsKey(property);
    final Object previousValue = entity.put(property, value);
    if (!Objects.equals(value, previousValue)) {
      getValueEditEvent(property.getPropertyId()).onEvent(valueChange(property, value, previousValue, initialization));
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable(final Property property) {
    return validator.isNullable(entity, property);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNull(final String propertyId) {
    return entity.isNull(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNotNull(final String propertyId) {
    return !entity.isNull(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getValidObserver() {
    return validState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return validState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final Property property) throws ValidationException {
    validator.validate(entity, property);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate() throws ValidationException {
    validate(entity);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final Entity entity) throws ValidationException {
    validate(singletonList(entity));
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final Collection<Entity> entities) throws ValidationException {
    for (final Entity entityToValidate : entities) {
      validator.validate(entityToValidate);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid(final Property property) {
    try {
      validator.validate(entity, requireNonNull(property, PROPERTY));
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Entity.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !getEntityDefinition().isKeyGenerated();
    final Entity toInsert = getEntityCopy(includePrimaryKeyValues);
    toInsert.saveAll();
    final List<Entity> insertedEntities = insertEntities(singletonList(toInsert));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    doSetEntity(insertedEntities.get(0));

    fireAfterInsertEvent(unmodifiableList(insertedEntities));

    return insertedEntities.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    final List<Entity> insertedEntities = insertEntities(entities);

    fireAfterInsertEvent(unmodifiableList(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity update() throws DatabaseException, ValidationException {
    final List<Entity> updated = update(singletonList(getEntityCopy()));
    if (updated.isEmpty()) {
      throw new UpdateException("Active entity is not modified");
    }

    return updated.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("This model does not allow updating!");
    }

    LOG.debug("{} - update {}", this, entities.toString());

    final List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return emptyList();
    }

    fireBeforeUpdateEvent(unmodifiableMap(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(entities))));
    validate(modifiedEntities);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      doSetEntity(updatedEntities.get(index));
    }

    fireAfterUpdateEvent(unmodifiableMap(mapToOriginalPrimaryKey(modifiedEntities, new ArrayList<>(updatedEntities))));

    return updatedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity delete() throws DatabaseException {
    final Entity originalEntity = getEntityCopy();
    originalEntity.revertAll();

    return delete(singletonList(originalEntity)).get(0);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException {
    requireNonNull(entities, ENTITIES);
    if (entities.isEmpty()) {
      return emptyList();
    }
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("This model does not allow deleting!");
    }

    LOG.debug("{} - delete {}", this, entities.toString());

    fireBeforeDeleteEvent(unmodifiableList(entities));

    final List<Entity> deleted = doDelete(entities);
    if (deleted.contains(getEntity())) {
      doSetEntity(null);
    }

    fireAfterDeleteEvent(unmodifiableList(deleted));

    return deleted;
  }

  /** {@inheritDoc} */
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
    lookupModel.getMultipleSelectionEnabledValue().set(false);

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
    return getDomain().defaultEntity(entity.getEntityId(), defaultValueProvider);
  }

  /** {@inheritDoc} */
  @Override
  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(connectionProvider, entity.getEntityId(), property.getPropertyId());
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
  public final boolean isPostEditEvents() {
    return postEditEvents;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setPostEditEvents(final boolean postEditEvents) {
    this.postEditEvents = postEditEvents;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueEditListener(final String propertyId, final EventDataListener listener) {
    if (valueEditEventMap.containsKey(propertyId)) {
      valueEditEventMap.get(propertyId).removeDataListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueEditListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    getValueEditEvent(propertyId).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final String propertyId, final EventDataListener listener) {
    if (valueChangeEventMap.containsKey(propertyId)) {
      valueChangeEventMap.get(propertyId).removeDataListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final String propertyId, final EventDataListener<ValueChange<Property, Object>> listener) {
    getValueChangeEvent(propertyId).addDataListener(listener);
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
  public final void addBeforeInsertListener(final EventDataListener<List<Entity>> listener) {
    beforeInsertEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterInsertListener(final EventDataListener listener) {
    afterInsertEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterInsertListener(final EventDataListener<List<Entity>> listener) {
    afterInsertEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeUpdateListener(final EventDataListener listener) {
    beforeUpdateEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
    beforeUpdateEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterUpdateListener(final EventDataListener listener) {
    afterUpdateEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterUpdateListener(final EventDataListener<Map<Entity.Key, Entity>> listener) {
    afterUpdateEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeDeleteListener(final EventDataListener<List<Entity>> listener) {
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
  public final void addAfterDeleteListener(final EventDataListener<List<Entity>> listener) {
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

  /**
   * Notifies that a insert is about to be performed
   * @param entities the entities about to be inserted
   * @see #addBeforeInsertListener(EventDataListener)
   */
  protected final void fireBeforeInsertEvent(final List<Entity> entities) {
    beforeInsertEvent.onEvent(entities);
  }

  /**
   * Notifies that a insert has been performed
   * @param insertedEntities the inserted entities
   * @see #addAfterInsertListener(EventDataListener)
   */
  protected final void fireAfterInsertEvent(final List<Entity> insertedEntities) {
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
  protected final void fireBeforeUpdateEvent(final Map<Entity.Key, Entity> entitiesToUpdate) {
    beforeUpdateEvent.onEvent(entitiesToUpdate);
  }

  /**
   * Notifies that an update has been performed
   * @param updatedEntities the updated entities
   * @see #addAfterUpdateListener(EventDataListener)
   */
  protected final void fireAfterUpdateEvent(final Map<Entity.Key, Entity> updatedEntities) {
    afterUpdateEvent.onEvent(updatedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyUpdated(updatedEntities);
    }
  }

  /**
   * Notifies that a delete is about to be performed
   * @param deleteEvent the entities about to be deleted
   * @see #addBeforeDeleteListener(EventDataListener)
   */
  protected final void fireBeforeDeleteEvent(final List<Entity> deleteEvent) {
    beforeDeleteEvent.onEvent(deleteEvent);
  }

  /**
   * Notifies that a delete has been performed
   * @param deletedEntities the deleted entities
   * @see #addAfterDeleteListener(EventDataListener)
   */
  protected final void fireAfterDeleteEvent(final List<Entity> deletedEntities) {
    afterDeleteEvent.onEvent(deletedEntities);
    if (postEditEvents) {
      EntityEditEvents.notifyDeleted(deletedEntities);
    }
  }

  private List<Entity> insertEntities(final List<Entity> entities) throws DatabaseException, ValidationException {
    if (!isInsertEnabled()) {
      throw new IllegalStateException("This model does not allow inserting!");
    }

    LOG.debug("{} - insert {}", this, entities.toString());

    fireBeforeInsertEvent(unmodifiableList(entities));
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

  private Event<ValueChange<Property, Object>> getValueEditEvent(final String propertyId) {
    return valueEditEventMap.computeIfAbsent(propertyId, k -> Events.event());
  }

  private Event<ValueChange<Property, Object>> getValueChangeEvent(final String propertyId) {
    return valueChangeEventMap.computeIfAbsent(propertyId, k -> Events.event());
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesChangedEvent);
    afterInsertEvent.addListener(entitiesChangedEvent);
    afterUpdateEvent.addListener(entitiesChangedEvent);
    entity.addValueListener(valueChange -> {
      validState.set(validator.isValid(entity));
      final Event<ValueChange<Property, Object>> valueChangeEvent = valueChangeEventMap.get(valueChange.getKey().getPropertyId());
      if (valueChangeEvent != null) {
        valueChangeEvent.onEvent(valueChange);
      }
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
      this.editModel.addValueListener(propertyId, valueChange -> fireChangeEvent(get()));
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