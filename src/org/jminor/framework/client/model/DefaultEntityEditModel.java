/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * </pre>
 */
public class DefaultEntityEditModel implements EntityEditModel {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private static final String FOREIGN_KEY_PROPERTY_ID = "foreignKeyPropertyID";
  private static final String FOREIGN_KEY_PROPERTY = "foreignKeyProperty";

  private final Event evtBeforeInsert = Events.event();
  private final Event evtAfterInsert = Events.event();
  private final Event evtBeforeUpdate = Events.event();
  private final Event evtAfterUpdate = Events.event();
  private final Event evtBeforeDelete = Events.event();
  private final Event evtAfterDelete = Events.event();
  private final Event evtEntitiesChanged = Events.event();
  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshDone = Events.event();

  private final State stPrimaryKeyNull = States.state(true);
  private final State stAllowInsert = States.state(true);
  private final State stAllowUpdate = States.state(true);
  private final State stAllowDelete = States.state(true);

  /**
   * The ID of the entity this edit model is based on
   */
  private final String entityID;

  /**
   * The {@link EntityConnectionProvider} instance to use
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel}, those that implement Refreshable
   * are refreshed when {@link #refreshComboBoxModels()} is called
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, FilteredComboBoxModel> propertyComboBoxModels = new HashMap<Property, FilteredComboBoxModel>();

  /**
   * Holds the EntityLookupModels used by this {@link EntityEditModel}
   */
  private final Map<Property.ForeignKeyProperty, EntityLookupModel> entityLookupModels =
          new HashMap<Property.ForeignKeyProperty, EntityLookupModel>();

  /**
   * Contains true if values should persist for the given property when the model is cleared
   */
  private final Map<String, Boolean> persistentValues = new HashMap<String, Boolean>();

  /**
   * The entity instance edited by this edit model.
   */
  private final Entity entity;

  /**
   * Fired when the active entity is set.
   * @see #setEntity(org.jminor.framework.domain.Entity)
   */
  private final Event evtEntitySet = Events.event();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<String, Event> valueSetEventMap = new HashMap<String, Event>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<String, Event> valueChangeEventMap = new HashMap<String, Event>();

  /**
   * The validator used by this edit model
   */
  private final Entity.Validator validator;

  /**
   * A state indicating whether or not the entity being edited is in a valid state
   * according the the validator
   */
  private final State stValid = States.state();

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
    return stAllowInsert.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowInsertObserver() {
    return stAllowInsert.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowUpdateObserver() {
    return stAllowUpdate.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getValueChangeObserver(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    if (!valueChangeEventMap.containsKey(propertyID)) {
      valueChangeEventMap.put(propertyID, Events.event());
    }

    return valueChangeEventMap.get(propertyID).getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getModifiedObserver() {
    return entity.getModifiedState();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getValidObserver() {
    return stValid.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getAllowDeleteObserver() {
    return stAllowDelete.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getPrimaryKeyNullObserver() {
    return stPrimaryKeyNull.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEntity(final Entity entity) {
    this.entity.setAs(entity == null ? getDefaultEntity() : entity);
    evtEntitySet.fire();
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
  public final Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
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
  public final Object getValue(final String propertyID) {
    return entity.getValue(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final void setValue(final String propertyID, final Object value) {
    Util.rejectNullValue(propertyID, "propertyID");
    final boolean initialization = !entity.containsValue(propertyID);
    final Object oldValue = entity.getValue(propertyID);
    entity.setValue(propertyID, prepareNewValue(propertyID, value));
    if (!Util.equal(value, oldValue)) {
      notifyValueSet(propertyID, new ValueChangeEvent<String, Object>(this, propertyID, value, oldValue, false, initialization));
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
    catch (ValidationException e) {
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
  public final boolean isEntityNew() {
    final Entity.Key key = entity.getPrimaryKey();
    final Entity.Key originalKey = entity.getOriginalPrimaryKey();
    return key.isNull() || originalKey.isNull();
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !Entities.isPrimaryKeyAutoGenerated(entityID);
    final List<Entity> insertedEntities = insertEntities(Arrays.asList(getEntityCopy(includePrimaryKeyValues)));
    final Entity.Key primaryKey = insertedEntities.get(0).getPrimaryKey();
    for (final Property.PrimaryKeyProperty primaryKeyProperty : primaryKey.getProperties()) {
      entity.setValue(primaryKeyProperty, primaryKey.getValue(primaryKeyProperty.getPropertyID()));
      entity.saveValue(primaryKeyProperty.getPropertyID());
    }

    evtAfterInsert.fire(new InsertEventImpl(insertedEntities));

    return insertedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException, ValidationException {
    final List<Entity> insertedEntities = insertEntities(entities);

    evtAfterInsert.fire(new InsertEventImpl(insertedEntities));

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

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return Collections.emptyList();
    }

    evtBeforeUpdate.fire();
    validator.validate(modifiedEntities);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    evtAfterUpdate.fire(new UpdateEventImpl(updatedEntities, EntityUtil.isPrimaryKeyModified(modifiedEntities)));

    return updatedEntities;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> delete() throws DatabaseException {
    return delete(Arrays.asList(getEntityCopy()));
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

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEventImpl(entities));

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    try {
      evtRefreshStarted.fire();
      refreshComboBoxModels();
    }
    finally {
      evtRefreshDone.fire();
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
  public final EntityLookupModel initializeEntityLookupModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, FOREIGN_KEY_PROPERTY_ID);
    return initializeEntityLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel initializeEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
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
    final EntityLookupModel entityLookupModel = entityLookupModels.get(foreignKeyProperty);
    if (entityLookupModel == null) {
      throw new IllegalStateException("No EntityLookupModel has been initialized for property: " + foreignKeyProperty);
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
  public final EntityComboBoxModel initializeEntityComboBoxModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, FOREIGN_KEY_PROPERTY_ID);
    return initializeEntityComboBoxModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
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
  public final FilteredComboBoxModel initializePropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                                     final String nullValueString) {
    Util.rejectNullValue(property, "property");
    FilteredComboBoxModel comboBoxModel = propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      comboBoxModel = createPropertyComboBoxModel(property, refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValueString);
      propertyComboBoxModels.put(property, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  @Override
  public FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                           final String nullValueString) {
    final FilteredComboBoxModel model = new DefaultPropertyComboBoxModel(entityID, connectionProvider, property, nullValueString, refreshEvent);
    model.refresh();

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, FOREIGN_KEY_PROPERTY);
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    model.setNullValueString(getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null);

    return model;
  }

  /** {@inheritDoc} */
  @Override
  public final FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, "property");
    final FilteredComboBoxModel comboBoxModel = propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      throw new IllegalStateException("No PropertyComboBoxModel has been initialized for property: " + property);
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
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      throw new IllegalStateException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);
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
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      defaultEntity.setValue(foreignKeyProperty, getDefaultValue(foreignKeyProperty));
    }

    return defaultEntity;
  }

  /** {@inheritDoc} */
  @Override
  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(connectionProvider, entityID, property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueSetListener(final String propertyID, final EventListener listener) {
    getValueSetEvent(propertyID).removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueSetListener(final String propertyID, final EventListener listener) {
    getValueSetEvent(propertyID).addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final String propertyID, final EventListener listener) {
    getValueChangeEvent(propertyID).removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final String propertyID, final EventListener listener) {
    getValueChangeObserver(propertyID).addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEntitySetListener(final EventListener listener) {
    evtEntitySet.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitySetListener(final EventListener listener) {
    evtEntitySet.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeInsertListener(final EventListener listener) {
    evtBeforeInsert.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeInsertListener(final EventListener listener) {
    evtBeforeInsert.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterInsertListener(final EventListener listener) {
    evtAfterInsert.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterInsertListener(final EventListener<InsertEvent> listener) {
    evtAfterInsert.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeUpdateListener(final EventListener listener) {
    evtBeforeUpdate.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeUpdateListener(final EventListener listener) {
    evtBeforeUpdate.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterUpdateListener(final EventListener listener) {
    evtAfterUpdate.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterUpdateListener(final EventListener<UpdateEvent> listener) {
    evtAfterUpdate.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeDeleteListener(final EventListener listener) {
    evtBeforeDelete.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeDeleteListener(final EventListener listener) {
    evtBeforeDelete.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterDeleteListener(final EventListener listener) {
    evtAfterDelete.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterDeleteListener(final EventListener<DeleteEvent> listener) {
    evtAfterDelete.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEntitiesChangedListener(final EventListener listener) {
    evtEntitiesChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitiesChangedListener(final EventListener listener) {
    evtEntitiesChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeRefreshListener(final EventListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    evtRefreshDone.removeListener(listener);
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
   * @return the actual {@link Entity} instance being edited
   */
  protected final Entity getEntity() {
    return entity;
  }

  /**
   * Provides a hook into the value setting mechanism, override to
   * translate or otherwise manipulate the value being set
   * @param propertyID the propertyID
   * @param value the value
   * @return the prepared value
   */
  protected Object prepareNewValue(final String propertyID, final Object value) {
    return value;
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

    evtBeforeInsert.fire();
    validator.validate(entities);

    return connectionProvider.getConnection().selectMany(doInsert(entities));
  }

  /**
   * Notifies that the value associated with the given propertyID has changed using the given event
   * @param propertyID the propertyID
   * @param event the event describing the value change
   */
  private void notifyValueSet(final String propertyID, final ValueChangeEvent event) {
    getValueSetEvent(propertyID).fire(event);
  }

  private Event getValueSetEvent(final String propertyID) {
    if (!valueSetEventMap.containsKey(propertyID)) {
      valueSetEventMap.put(propertyID, Events.event());
    }

    return valueSetEventMap.get(propertyID);
  }

  private Event getValueChangeEvent(final String propertyID) {
    if (!valueChangeEventMap.containsKey(propertyID)) {
      valueChangeEventMap.put(propertyID, Events.event());
    }

    return valueChangeEventMap.get(propertyID);
  }

  private void bindEventsInternal() {
    evtAfterDelete.addListener(evtEntitiesChanged);
    evtAfterInsert.addListener(evtEntitiesChanged);
    evtAfterUpdate.addListener(evtEntitiesChanged);
    entity.addValueListener(new ValueChangeListener<String, Object>() {
      @Override
      protected void valueChanged(final ValueChangeEvent<String, Object> event) {
        stPrimaryKeyNull.setActive(entity.isPrimaryKeyNull());
        stValid.setActive(validator.isValid(entity));
        final Event valueChangeEvent = valueChangeEventMap.get(event.getKey());
        if (valueChangeEvent != null) {
          valueChangeEvent.fire(event);
        }
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      for (final Property property : Entities.getProperties(entityID).values()) {
        addValueSetListener(property.getPropertyID(), new StatusMessageListener());
      }
      entity.addValueListener(new StatusMessageListener());
    }
  }

  private boolean containsLookupModel(final Property.ForeignKeyProperty property) {
    return entityLookupModels.containsKey(property);
  }

  private boolean containsComboBoxModel(final Property property) {
    return propertyComboBoxModels.containsKey(property);
  }

  private String getValueChangeDebugString(final ValueChangeEvent<String, Object> event) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (event.getSource() instanceof Entity) {
      stringBuilder.append("[entity] ");
    }
    else {
      stringBuilder.append(event.isModelChange() ? "[model] " : "[ui] ");
    }
    final Property property = Entities.getProperty(getEntityID(), event.getKey());
    final boolean isForeignKeyProperty = property instanceof Property.ColumnProperty
            && ((Property.ColumnProperty) property).isForeignKeyProperty();
    stringBuilder.append(getEntityID()).append(" : ").append(property).append(
            isForeignKeyProperty ? " [fk]" : "").append("; ");
    if (!event.isInitialization()) {
      if (!event.isOldValueNull()) {
        stringBuilder.append(event.getOldValue().getClass().getSimpleName()).append(" ");
      }
      stringBuilder.append(getValueString(event.getOldValue()));
    }
    if (!event.isInitialization()) {
      stringBuilder.append(" -> ");
    }
    if (!event.isNewValueNull()) {
      stringBuilder.append(event.getNewValue().getClass().getSimpleName()).append(" ");
    }
    stringBuilder.append(getValueString(event.getNewValue()));

    return stringBuilder.toString();
  }

  /**
   * @param value the value
   * @return a string representing the given property value for debug output
   */
  private static String getValueString(final Object value) {
    final StringBuilder stringBuilder = new StringBuilder("[").append(value == null ? "null value" : value).append("]");
    if (value instanceof Entity) {
      stringBuilder.append(" PK{").append(((Entity) value).getPrimaryKey()).append("}");
    }

    return stringBuilder.toString();
  }

  private final class StatusMessageListener extends ValueChangeListener<String, Object> {
    /** {@inheritDoc} */
    @Override
    protected void valueChanged(final ValueChangeEvent<String, Object> event) {
      final String msg = getValueChangeDebugString(event);
      System.out.println(msg);
      LOG.debug(msg);
    }
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

    /** {@inheritDoc} */
    @Override
    public Collection<Object> getValues() {
      try {
        return connectionProvider.getConnection().selectPropertyValues(entityID, propertyID, true);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class InsertEventImpl implements InsertEvent {

    private final List<Entity> insertedEntities;

    /**
     * Instantiates a new InsertEventImpl.
     * @param insertedEntities the inserted entities
     */
    private InsertEventImpl(final List<Entity> insertedEntities) {
      this.insertedEntities = insertedEntities;
    }

    /** {@inheritDoc} */
    @Override
    public List<Entity> getInsertedEntities() {
      return insertedEntities;
    }
  }

  private static final class DeleteEventImpl implements DeleteEvent {

    private final List<Entity> deletedEntities;

    /**
     * Instantiates a new DeleteEventImpl.
     * @param deletedEntities the deleted entities
     */
    private DeleteEventImpl(final List<Entity> deletedEntities) {
      this.deletedEntities = deletedEntities;
    }

    /** {@inheritDoc} */
    @Override
    public List<Entity> getDeletedEntities() {
      return deletedEntities;
    }
  }

  private static final class UpdateEventImpl implements UpdateEvent {

    private final List<Entity> updatedEntities;
    private final boolean primaryKeyModified;

    /**
     * Instantiates a new UpdateEventImpl.
     * @param updatedEntities the updated entities
     * @param primaryKeyModified true if primary key values were modified during the update
     */
    private UpdateEventImpl(final List<Entity> updatedEntities, final boolean primaryKeyModified) {
      this.updatedEntities = updatedEntities;
      this.primaryKeyModified = primaryKeyModified;
    }

    /** {@inheritDoc} */
    @Override
    public List<Entity> getUpdatedEntities() {
      return updatedEntities;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrimaryKeyModified() {
      return primaryKeyModified;
    }
  }
}