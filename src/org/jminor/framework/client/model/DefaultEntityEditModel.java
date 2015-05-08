/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.common.model.valuemap.ValueChanges;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A default {@link EntityEditModel} implementation
 *
 * <pre>
 * String entityID = "some.entity";
 * String clientTypeID = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeID);
 *
 * EntityEditModel editModel = new DefaultEntityEditModel(entityID, connectionProvider);
 *
 * EntityEditPanel panel = new EntityEditPanel(editModel);
 * panel.initializePanel();
 * </pre>
 */
public class DefaultEntityEditModel implements EntityEditModel {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private static final String FOREIGN_KEY_PROPERTY_ID = "foreignKeyPropertyID";
  private static final String FOREIGN_KEY_PROPERTY = "foreignKeyProperty";
  private static final String PROPERTY = "property";

  private final Event beforeInsertEvent = Events.event();
  private final Event<InsertEvent> afterInsertEvent = Events.event();
  private final Event beforeUpdateEvent = Events.event();
  private final Event<UpdateEvent> afterUpdateEvent = Events.event();
  private final Event beforeDeleteEvent = Events.event();
  private final Event<DeleteEvent> afterDeleteEvent = Events.event();
  private final Event entitiesChangedEvent = Events.event();
  private final Event refreshStartedEvent = Events.event();
  private final Event refreshDoneEvent = Events.event();
  private final Event<State> confirmSetEntityEvent = Events.event();

  private final State primaryKeyNullState = States.state(true);
  private final State allowInsertState = States.state(true);
  private final State allowUpdateState = States.state(true);
  private final State allowDeleteState = States.state(true);

  /**
   * The ID of the entity this edit model is based on
   */
  private final String entityID;

  /**
   * The {@link EntityConnectionProvider} instance to use
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel},
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, FilteredComboBoxModel> propertyComboBoxModels = new HashMap<>();

  /**
   * Holds the EntityLookupModels used by this {@link EntityEditModel}
   */
  private final Map<Property.ForeignKeyProperty, EntityLookupModel> entityLookupModels = new HashMap<>();

  /**
   * Contains true if values should persist for the given property when the model is cleared
   */
  private final Map<String, Boolean> persistentValues = new HashMap<>();

  /**
   * The entity instance edited by this edit model.
   */
  private final Entity entity;

  /**
   * Fired when the active entity is set.
   * @see #setEntity(org.jminor.framework.domain.Entity)
   */
  private final Event<Entity> entitySetEvent = Events.event();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<String, Event<ValueChange<String, ?>>> valueSetEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<String, Event<ValueChange<String, ?>>> valueChangeEventMap = new HashMap<>();

  /**
   * The validator used by this edit model
   */
  private final Entity.Validator validator;

  /**
   * A state indicating whether or not the entity being edited is in a valid state according the the validator
   */
  private final State validState = States.state();

  /**
   * A state indicating whether or not the entity being edited is new
   * @see #isEntityNew()
   */
  private final State entityNewState = States.state(true);

  /**
   * Holds the read only status of this edit model
   */
  private boolean readOnly;

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public DefaultEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, Entities.getValidator(entityID));
  }

  /**
   * Instantiates a new {@link DefaultEntityEditModel} based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider, final Entity.Validator validator) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.entityID = entityID;
    this.entity = Entities.entity(entityID);
    this.connectionProvider = connectionProvider;
    this.validator = validator;
    this.readOnly = Entities.isReadOnly(entityID);
    setEntity(null);
    bindEventsInternal();
  }

  @Override
  public final String toString() {
    return getClass().toString() + ", " + getEntityID();
  }

  /** {@inheritDoc} */
  @Override
  public Object getDefaultValue(final Property property) {
    return isValuePersistent(property) ? getValue(property.getPropertyID()) : property.getDefaultValue();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
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
    if (persistentValues.containsKey(property.getPropertyID())) {
      return persistentValues.get(property.getPropertyID());
    }

    return property instanceof Property.ForeignKeyProperty &&
            Configuration.getBooleanValue(Configuration.PERSIST_FOREIGN_KEY_VALUES);
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setValuePersistent(final String propertyID, final boolean persistValue) {
    persistentValues.put(propertyID, persistValue);
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
  public final EventObserver<ValueChange<String, ?>> getValueChangeObserver() {
    return entity.getValueChangeObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<String, ?>> getValueChangeObserver(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    return getValueChangeEvent(propertyID).getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getModifiedObserver() {
    return entity.getModifiedObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getEntityNewObserver() {
    return entityNewState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getValidObserver() {
    return validState.getObserver();
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
      this.entity.setAs(entity == null ? getDefaultEntity() : entity);
      entitySetEvent.fire(entity);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(this.entityID, foreignKeyEntityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
        getEntityComboBoxModel(foreignKeyProperty.getPropertyID()).refresh();
      }
      final Entity currentForeignKeyValue = getForeignKeyValue(foreignKeyProperty.getPropertyID());
      if (currentForeignKeyValue != null) {
        for (final Entity newForeignKeyValue : foreignKeyValues) {
          if (currentForeignKeyValue.equals(newForeignKeyValue)) {
            setValue(foreignKeyProperty.getPropertyID(), null);
            setValue(foreignKeyProperty.getPropertyID(), newForeignKeyValue);
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
      copy.clearPrimaryKeyValues();
    }

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable(final String propertyID) {
    return validator.isNullable(entity, propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final String propertyID) throws ValidationException {
    validator.validate(entity, propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate() throws ValidationException {
    validate(entity);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final Collection<? extends ValueMap<String, Object>> valueMaps) throws ValidationException {
    for (final ValueMap<String, Object> entityToValidate : valueMaps) {
      validate((Entity) entityToValidate);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Object getValue(final String propertyID) {
    return entity.getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final void setValue(final String propertyID, final Object value) {
    Util.rejectNullValue(propertyID, "propertyID");
    final boolean initialization = !entity.containsValue(propertyID);
    final Object oldValue = entity.getValue(propertyID);
    entity.setValue(propertyID, prepareNewValue(propertyID, value));
    if (!Util.equal(value, oldValue)) {
      notifyValueChange(propertyID, ValueChanges.valueChange(this, propertyID, value, oldValue, initialization));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValueNull(final String propertyID) {
    return entity.isValueNull(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    try {
      validator.validate(entity, propertyID);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified() {
    return getModifiedObserver().isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return getValidObserver().isActive();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEntityNew() {
    final Entity.Key key = entity.getPrimaryKey();
    final Entity.Key originalKey = entity.getOriginalPrimaryKey();
    return key.isNull() || originalKey.isNull();
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !Entities.isPrimaryKeyAutoGenerated(entityID);
    final List<Entity> insertedEntities = insertEntities(Arrays.asList(getEntityCopy(includePrimaryKeyValues)));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    final Entity.Key primaryKey = insertedEntities.get(0).getPrimaryKey();
    for (final Property.ColumnProperty primaryKeyProperty : primaryKey.getProperties()) {
      entity.setValue(primaryKeyProperty, primaryKey.getValue(primaryKeyProperty.getPropertyID()));
      entity.saveValue(primaryKeyProperty.getPropertyID());
    }

    afterInsertEvent.fire(new DefaultInsertEvent(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException {
    final List<Entity> insertedEntities = insertEntities(entities);

    afterInsertEvent.fire(new DefaultInsertEvent(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update() throws DatabaseException, ValidationException {
    return update(Arrays.asList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    if (readOnly) {
      throw new UnsupportedOperationException("This is a read-only model, updating is not allowed!");
    }
    if (!isUpdateAllowed()) {
      throw new UnsupportedOperationException("This model does not allow updating!");
    }

    LOG.debug("{} - update {}", this, Util.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return Collections.emptyList();
    }

    beforeUpdateEvent.fire();
    validate(modifiedEntities);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    afterUpdateEvent.fire(new DefaultUpdateEvent(getOriginalKeyMap(modifiedEntities, new ArrayList<>(updatedEntities))));

    return updatedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete() throws DatabaseException {
    return delete(Arrays.asList((Entity) getEntity().getOriginalCopy()));
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    if (readOnly) {
      throw new UnsupportedOperationException("This is a read-only model, deleting is not allowed!");
    }
    if (!isDeleteAllowed()) {
      throw new UnsupportedOperationException("This model does not allow deleting!");
    }

    LOG.debug("{} - delete {}", this, Util.getCollectionContentsAsString(entities, false));

    beforeDeleteEvent.fire();

    doDelete(entities);
    if (entities.contains(getEntity())) {
      setEntity(null);
    }

    afterDeleteEvent.fire(new DefaultDeleteEvent(entities));

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    try {
      refreshStartedEvent.fire();
      refreshComboBoxModels();
    }
    finally {
      refreshDoneEvent.fire();
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    clearComboBoxModels();
  }

  /** {@inheritDoc} */
  @Override
  public final void refreshComboBoxModels() {
    for (final FilteredComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      comboBoxModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clearComboBoxModels() {
    for (final FilteredComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      comboBoxModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsLookupModel(final String foreignKeyPropertyID) {
    return containsLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  @Override
  public EntityLookupModel createEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
    if (searchProperties.isEmpty()) {
      throw new IllegalStateException("No search properties defined for entity: " + foreignKeyProperty.getReferencedEntityID());
    }

    return new DefaultEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider, searchProperties);
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getEntityLookupModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, FOREIGN_KEY_PROPERTY_ID);
    return getEntityLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, FOREIGN_KEY_PROPERTY);
    EntityLookupModel entityLookupModel = entityLookupModels.get(foreignKeyProperty);
    if (entityLookupModel == null) {
      entityLookupModel = createEntityLookupModel(foreignKeyProperty);
      entityLookupModels.put(foreignKeyProperty, entityLookupModel);
    }

    return entityLookupModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, PROPERTY);
    final FilteredComboBoxModel<Object> model = new DefaultPropertyComboBoxModel<>(entityID, connectionProvider, property, null, entitiesChangedEvent);
    model.setNullValue(getValidator().isNullable(getEntity(), property.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM) : null);
    model.refresh();

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, FOREIGN_KEY_PROPERTY);
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    if (getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID())) {
      model.setNullValue(EntityUtil.createToStringEntity(entityID, (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM)));
    }

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public final FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, PROPERTY);
    FilteredComboBoxModel comboBoxModel = propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      comboBoxModel = createPropertyComboBoxModel(property);
      propertyComboBoxModels.put(property, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel getEntityComboBoxModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, FOREIGN_KEY_PROPERTY_ID);
    return getEntityComboBoxModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, FOREIGN_KEY_PROPERTY);
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      comboBoxModel = createEntityComboBoxModel(foreignKeyProperty);
      propertyComboBoxModels.put(foreignKeyProperty, comboBoxModel);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getDefaultEntity() {
    final Entity defaultEntity = Entities.entity(entityID);
    final Collection<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
        defaultEntity.setValue(property, getDefaultValue(property));
      }
    }
    final Collection<Property.TransientProperty> transientProperties = Entities.getTransientProperties(entityID);
    for (final Property.TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof Property.DerivedProperty) && !(transientProperty instanceof Property.DenormalizedViewProperty)) {
        defaultEntity.setValue(transientProperty, getDefaultValue(transientProperty));
      }
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      defaultEntity.setValue(foreignKeyProperty, getDefaultValue(foreignKeyProperty));
    }
    defaultEntity.saveAll();

    return defaultEntity;
  }

  /** {@inheritDoc} */
  @Override
  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(connectionProvider, entityID, property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsUnsavedData() {
    if (isEntityNew()) {
      for (final Property.ColumnProperty property : Entities.getColumnProperties(getEntityID())) {
        if (!property.isForeignKeyProperty() && valueModified(property)) {
          return true;
        }
      }
      for (final Property.ForeignKeyProperty property : Entities.getForeignKeyProperties(getEntityID())) {
        if (valueModified(property)) {
          return true;
        }
      }

      return false;
    }
    else {
      return !getEntity().getOriginalValueKeys().isEmpty();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueSetListener(final String propertyID, final EventInfoListener listener) {
    if (valueSetEventMap.containsKey(propertyID)) {
      valueSetEventMap.get(propertyID).removeInfoListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueSetListener(final String propertyID, final EventInfoListener<ValueChange<String, ?>> listener) {
    getValueSetEvent(propertyID).addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final String propertyID, final EventInfoListener listener) {
    if (valueChangeEventMap.containsKey(propertyID)) {
      valueChangeEventMap.get(propertyID).removeInfoListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final String propertyID, final EventInfoListener<ValueChange<String, ?>> listener) {
    getValueChangeObserver(propertyID).addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEntitySetListener(final EventInfoListener listener) {
    entitySetEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitySetListener(final EventInfoListener<Entity> listener) {
    entitySetEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeInsertListener(final EventListener listener) {
    beforeInsertEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeInsertListener(final EventListener listener) {
    beforeInsertEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterInsertListener(final EventInfoListener listener) {
    afterInsertEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterInsertListener(final EventInfoListener<InsertEvent> listener) {
    afterInsertEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeUpdateListener(final EventListener listener) {
    beforeUpdateEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeUpdateListener(final EventListener listener) {
    beforeUpdateEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterUpdateListener(final EventInfoListener listener) {
    afterUpdateEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterUpdateListener(final EventInfoListener<UpdateEvent> listener) {
    afterUpdateEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeDeleteListener(final EventListener listener) {
    beforeDeleteEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeDeleteListener(final EventListener listener) {
    beforeDeleteEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterDeleteListener(final EventInfoListener listener) {
    afterDeleteEvent.removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterDeleteListener(final EventInfoListener<DeleteEvent> listener) {
    afterDeleteEvent.addInfoListener(listener);
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
    refreshStartedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addConfirmSetEntityObserver(final EventInfoListener<State> listener) {
    confirmSetEntityEvent.addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeConfirmSetEntityObserver(final EventInfoListener listener) {
    confirmSetEntityEvent.removeInfoListener(listener);
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
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   */
  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().insert(entities);
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   */
  protected List<Entity> doUpdate(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().update(entities);
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   */
  protected void doDelete(final List<Entity> entities) throws DatabaseException {
    connectionProvider.getConnection().delete(EntityUtil.getPrimaryKeys(entities));
  }

  /**
   * Provides a hook into the value setting mechanism, override to translate or otherwise manipulate the value being set
   * @param propertyID the propertyID
   * @param value the value
   * @return the prepared value
   */
  protected Object prepareNewValue(final String propertyID, final Object value) {
    return value;
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
    return EntityUtil.getModifiedEntities(entities);
  }

  /**
   * Validates the given entity.
   * By default the internal validator is used.
   * @param entity the entity to validate
   * @throws ValidationException
   * @see #getValidator()
   */
  protected void validate(final Entity entity) throws ValidationException {
    validator.validate(entity);
  }

  private List<Entity> insertEntities(final List<Entity> entities) throws DatabaseException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    if (readOnly) {
      throw new UnsupportedOperationException("This is a read-only model, inserting is not allowed!");
    }
    if (!isInsertAllowed()) {
      throw new UnsupportedOperationException("This model does not allow inserting!");
    }

    LOG.debug("{} - insert {}", this, Util.getCollectionContentsAsString(entities, false));

    beforeInsertEvent.fire();
    validate(entities);

    return connectionProvider.getConnection().selectMany(doInsert(entities));
  }

  /**
   * Notifies that the value associated with the given propertyID has changed using the given event
   * @param propertyID the propertyID
   * @param event the event describing the value change
   */
  private void notifyValueChange(final String propertyID, final ValueChange<String, ?> event) {
    getValueSetEvent(propertyID).fire(event);
  }

  private Event<ValueChange<String, ?>> getValueSetEvent(final String propertyID) {
    if (!valueSetEventMap.containsKey(propertyID)) {
      valueSetEventMap.put(propertyID, Events.<ValueChange<String, ?>>event());
    }

    return valueSetEventMap.get(propertyID);
  }

  private Event<ValueChange<String, ?>> getValueChangeEvent(final String propertyID) {
    if (!valueChangeEventMap.containsKey(propertyID)) {
      valueChangeEventMap.put(propertyID, Events.<ValueChange<String, ?>>event());
    }

    return valueChangeEventMap.get(propertyID);
  }

  private boolean isSetEntityAllowed() {
    if (Configuration.getBooleanValue(Configuration.WARN_ABOUT_UNSAVED_DATA) && containsUnsavedData()) {
      final State confirmation = States.state(true);
      confirmSetEntityEvent.fire(confirmation);

      return confirmation.isActive();
    }

    return true;
  }

  private boolean valueModified(final Property property) {
    return !Util.equal(getValue(property.getPropertyID()), getDefaultValue(property));
  }

  private void bindEventsInternal() {
    afterDeleteEvent.addListener(entitiesChangedEvent);
    afterInsertEvent.addListener(entitiesChangedEvent);
    afterUpdateEvent.addListener(entitiesChangedEvent);
    entity.addValueListener(new EventInfoListener<ValueChange<String, ?>>() {
      @Override
      public void eventOccurred(final ValueChange<String, ?> info) {
        primaryKeyNullState.setActive(entity.isPrimaryKeyNull());
        validState.setActive(validator.isValid(entity));
        entityNewState.setActive(isEntityNew());
        final Event<ValueChange<String, ?>> valueChangeEvent = valueChangeEventMap.get(info.getKey());
        if (valueChangeEvent != null) {
          valueChangeEvent.fire(info);
        }
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      entity.addValueListener(new EventInfoListener<ValueChange<String, ?>>() {
        @Override
        public void eventOccurred(final ValueChange<String, ?> info) {
          final String msg = getValueChangeDebugString(getEntityID(), info);
          System.out.println(msg);
          LOG.debug(msg);
        }
      });
    }
  }

  private boolean containsLookupModel(final Property.ForeignKeyProperty property) {
    return entityLookupModels.containsKey(property);
  }

  private boolean containsComboBoxModel(final Property property) {
    return propertyComboBoxModels.containsKey(property);
  }

  private static String getValueChangeDebugString(final String entityID, final ValueChange<String, ?> event) {
    final StringBuilder stringBuilder = new StringBuilder();
    final Property property = Entities.getProperty(entityID, event.getKey());
    final boolean isForeignKeyProperty = property instanceof Property.ColumnProperty
            && ((Property.ColumnProperty) property).isForeignKeyProperty();
    stringBuilder.append(entityID).append("#").append(property).append(isForeignKeyProperty ? " [fk]" : "").append(": ");
    if (!event.isInitialization()) {
      stringBuilder.append(getValueString(event.getOldValue()));
      stringBuilder.append(" -> ");
    }
    stringBuilder.append(getValueString(event.getNewValue()));

    return stringBuilder.toString();
  }

  /**
   * @param value the value
   * @return a string representing the given property value for debug output
   */
  private static String getValueString(final Object value) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (value != null) {
      stringBuilder.append(value.getClass().getSimpleName()).append(" ");
    }
    stringBuilder.append("[").append(value == null ? "null" : value).append("]");
    if (value instanceof Entity) {
      stringBuilder.append(" PK{").append(((Entity) value).getPrimaryKey()).append("}");
    }

    return stringBuilder.toString();
  }

  /**
   * @param entitiesBeforeUpdate the entities before update
   * @param entitiesAfterUpdate the entities after update
   * @return the updated entities mapped to their respective original primary keys
   */
  private static Map<Entity.Key, Entity> getOriginalKeyMap(final List<Entity> entitiesBeforeUpdate,
                                                           final List<Entity> entitiesAfterUpdate) {
    final Map<Entity.Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
    for (final Entity entity : entitiesBeforeUpdate) {
      keyMap.put(entity.getOriginalPrimaryKey(), findAndRemove(entity.getPrimaryKey(), entitiesAfterUpdate.listIterator()));
    }

    return keyMap;
  }

  private static Entity findAndRemove(final Entity.Key primaryKey, final ListIterator<Entity> iterator) {
    while (iterator.hasNext()) {
      final Entity current = iterator.next();
      if (current.getPrimaryKey().equals(primaryKey)) {
        iterator.remove();

        return current;
      }
    }

    return null;
  }

  static final class PropertyValueProvider implements ValueCollectionProvider<Object> {

    private final EntityConnectionProvider connectionProvider;
    private final String entityID;
    private final String propertyID;

    private PropertyValueProvider(final EntityConnectionProvider connectionProvider, final String entityID, final String propertyID) {
      this.connectionProvider = connectionProvider;
      this.entityID = entityID;
      this.propertyID = propertyID;
    }

    @Override
    public Collection<Object> getValues() {
      try {
        return connectionProvider.getConnection().selectPropertyValues(entityID, propertyID, true);
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class DefaultInsertEvent implements InsertEvent {

    private final List<Entity> insertedEntities;

    /**
     * Instantiates a new DefaultInsertEvent.
     * @param insertedEntities the inserted entities
     */
    private DefaultInsertEvent(final List<Entity> insertedEntities) {
      this.insertedEntities = insertedEntities;
    }

    @Override
    public List<Entity> getInsertedEntities() {
      return insertedEntities;
    }
  }

  private static final class DefaultDeleteEvent implements DeleteEvent {

    private final List<Entity> deletedEntities;

    /**
     * Instantiates a new DefaultDeleteEvent.
     * @param deletedEntities the deleted entities
     */
    private DefaultDeleteEvent(final List<Entity> deletedEntities) {
      this.deletedEntities = deletedEntities;
    }

    @Override
    public List<Entity> getDeletedEntities() {
      return deletedEntities;
    }
  }

  private static final class DefaultUpdateEvent implements UpdateEvent {

    private final Map<Entity.Key, Entity> updatedEntities;

    /**
     * Instantiates a new DefaultUpdateEvent.
     * @param updatedEntities the updated entities, mapped to their respective original primary key, that is,
     * the primary key prior to the update
     */
    private DefaultUpdateEvent(final Map<Entity.Key, Entity> updatedEntities) {
      this.updatedEntities = updatedEntities;
    }

    @Override
    public Map<Entity.Key, Entity> getUpdatedEntities() {
      return updatedEntities;
    }
  }
}