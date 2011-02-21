/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.common.model.valuemap.AbstractValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class DefaultEntityEditModel extends AbstractValueChangeMapEditModel<String, Object> implements EntityEditModel {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityEditModel.class);

  private final Event evtBeforeInsert = Events.event();
  private final Event evtAfterInsert = Events.event();
  private final Event evtBeforeUpdate = Events.event();
  private final Event evtAfterUpdate = Events.event();
  private final Event evtBeforeDelete = Events.event();
  private final Event evtAfterDelete = Events.event();
  private final Event evtEntitiesChanged = Events.event();
  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshDone = Events.event();

  private final State stEntityNull = States.state(true);
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
   * Contains true if values should be persisted for the given property when the model is cleared
   */
  private final Map<String, Boolean> persistingValues = new HashMap<String, Boolean>();

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
    super(Entities.entity(entityID), validator);
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.readOnly = Entities.isReadOnly(entityID);
    setValueMap(null);
    bindEventsInternal();
  }

  @Override
  public final String toString() {
    return getClass().toString() + ", " + getEntityID();
  }

  /** {@inheritDoc} */
  public Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property.getPropertyID()) : property.getDefaultValue();
  }

  /** {@inheritDoc} */
  public boolean persistValueOnClear(final Property property) {
    if (persistingValues.containsKey(property.getPropertyID())) {
      return persistingValues.get(property.getPropertyID());
    }
    return property instanceof Property.ForeignKeyProperty &&
            Configuration.getBooleanValue(Configuration.PERSIST_FOREIGN_KEY_VALUES);
  }

  /** {@inheritDoc} */
  public final boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  public final EntityEditModel setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /** {@inheritDoc} */
  public final EntityEditModel setPersistValueOnClear(final String propertyID, final boolean persistValueOnClear) {
    persistingValues.put(propertyID, persistValueOnClear);
    return this;
  }

  /** {@inheritDoc} */
  public final boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  /** {@inheritDoc} */
  public final EntityEditModel setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  public final StateObserver getAllowInsertObserver() {
    return stAllowInsert.getObserver();
  }

  /** {@inheritDoc} */
  public final boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  /** {@inheritDoc} */
  public final EntityEditModel setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  public final StateObserver getAllowUpdateObserver() {
    return stAllowUpdate.getObserver();
  }

  /** {@inheritDoc} */
  public final boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  /** {@inheritDoc} */
  public final EntityEditModel setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  public final StateObserver getAllowDeleteObserver() {
    return stAllowDelete.getObserver();
  }

  /** {@inheritDoc} */
  public final StateObserver getEntityNullObserver() {
    return stEntityNull.getObserver();
  }

  /** {@inheritDoc} */
  public final void setEntity(final Entity entity) {
    setValueMap(entity);
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  public final Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  public final void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> newForeignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(this.entityID, foreignKeyEntityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
        getEntityComboBoxModel(foreignKeyProperty.getPropertyID()).refresh();
      }
      final Entity currentForeignKeyValue = getForeignKeyValue(foreignKeyProperty.getPropertyID());
      if (currentForeignKeyValue != null) {
        for (final Entity newForeignKeyValue : newForeignKeyValues) {
          if (currentForeignKeyValue.equals(newForeignKeyValue)) {
            setValue(foreignKeyProperty.getPropertyID(), null);
            setValue(foreignKeyProperty.getPropertyID(), newForeignKeyValue);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  public final Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  /** {@inheritDoc} */
  public final Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includePrimaryKeyValues) {
      copy.clearPrimaryKeyValues();
    }

    return copy;
  }

  /** {@inheritDoc} */
  public final boolean isEntityNew() {
    final Entity.Key key = ((Entity) getValueMap()).getPrimaryKey();
    final Entity.Key originalKey = ((Entity) getValueMap()).getOriginalPrimaryKey();
    return key.isNull() || originalKey.isNull();
  }

  /** {@inheritDoc} */
  public final List<Entity.Key> insert() throws CancelException, DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !Entities.isPrimaryKeyAutoGenerated(entityID);
    return insert(Arrays.asList(getEntityCopy(includePrimaryKeyValues)));
  }

  /** {@inheritDoc} */
  public final List<Entity.Key> insert(final List<Entity> entities) throws CancelException, DatabaseException, ValidationException {
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

    LOG.debug(toString() + " - insert " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeInsert.fire();
    ((Entity.Validator) getValidator()).validate(entities, Entity.Validator.INSERT);

    final List<Entity.Key> primaryKeys = doInsert(entities);
    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));

    return primaryKeys;
  }

  /** {@inheritDoc} */
  public final List<Entity> update() throws CancelException, DatabaseException, ValidationException {
    return update(Arrays.asList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  public final List<Entity> update(final List<Entity> entities) throws DatabaseException, CancelException, ValidationException {
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

    LOG.debug(toString() + " - update " + Util.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return Collections.emptyList();
    }

    evtBeforeUpdate.fire();
    ((Entity.Validator) getValidator()).validate(modifiedEntities, Entity.Validator.UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities, EntityUtil.isPrimaryKeyModified(modifiedEntities)));

    return updatedEntities;
  }

  /** {@inheritDoc} */
  public final List<Entity> delete() throws DatabaseException, CancelException {
    return delete(Arrays.asList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  public final List<Entity> delete(final List<Entity> entities) throws DatabaseException, CancelException {
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

    LOG.debug(toString() + " - delete " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));

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
  public final void refreshComboBoxModels() {
    for (final FilteredComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      comboBoxModel.refresh();
    }
  }

  /** {@inheritDoc} */
  public final void clearComboBoxModels() {
    for (final FilteredComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      comboBoxModel.clear();
    }
  }

  /** {@inheritDoc} */
  public final boolean containsLookupModel(final String foreignKeyPropertyID) {
    return containsLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public final EntityLookupModel initializeEntityLookupModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, "foreignKeyPropertyID");
    return initializeEntityLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public final EntityLookupModel initializeEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    EntityLookupModel entityLookupModel = entityLookupModels.get(foreignKeyProperty);
    if (entityLookupModel == null) {
      entityLookupModel = createEntityLookupModel(foreignKeyProperty);
      entityLookupModels.put(foreignKeyProperty, entityLookupModel);
    }

    return entityLookupModel;
  }

  /** {@inheritDoc} */
  public EntityLookupModel createEntityLookupModel(final String foreignKeyPropertyID) {
    return createEntityLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public EntityLookupModel createEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
    if (searchProperties.isEmpty()) {
      throw new IllegalStateException("No search properties defined for entity: " + entityID);
    }

    return new DefaultEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider, searchProperties);
  }

  /** {@inheritDoc} */
  public final EntityLookupModel getEntityLookupModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, "foreignKeyPropertyID");
    return getEntityLookupModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public final EntityLookupModel getEntityLookupModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityLookupModel entityLookupModel = entityLookupModels.get(foreignKeyProperty);
    if (entityLookupModel == null) {
      throw new IllegalStateException("No EntityLookupModel has been initialized for property: " + foreignKeyProperty);
    }

    return entityLookupModel;
  }

  /** {@inheritDoc} */
  public final boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel initializeEntityComboBoxModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, "foreignKeyPropertyID");
    return initializeEntityComboBoxModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      comboBoxModel = createEntityComboBoxModel(foreignKeyProperty);
      propertyComboBoxModels.put(foreignKeyProperty, comboBoxModel);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
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
  public FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                           final String nullValueString) {
    final FilteredComboBoxModel model = new DefaultPropertyComboBoxModel(entityID, connectionProvider, property, nullValueString, refreshEvent);
    model.refresh();

    return model;
  }

  /** {@inheritDoc} */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), connectionProvider);
    model.setNullValueString(getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null);

    return model;
  }

  /** {@inheritDoc} */
  public final FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, "property");
    final FilteredComboBoxModel comboBoxModel = propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      throw new IllegalStateException("No PropertyComboBoxModel has been initialized for property: " + property);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel getEntityComboBoxModel(final String foreignKeyPropertyID) {
    Util.rejectNullValue(foreignKeyPropertyID, "foreignKeyPropertyID");
    return getEntityComboBoxModel(Entities.getForeignKeyProperty(entityID, foreignKeyPropertyID));
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      throw new IllegalStateException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  public final Entity getDefaultValueMap() {
    final Entity defaultEntity = Entities.entity(entityID);
    final Collection<Property.ColumnProperty> columnProperties = Entities.getColumnProperties(entityID);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.hasParentProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
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
  public final ValueCollectionProvider<Object> getValueProvider(final Property property) {
    return new PropertyValueProvider(connectionProvider, entityID, property.getPropertyID());
  }

  /** {@inheritDoc} */
  public final void removeBeforeInsertListener(final ActionListener listener) {
    evtBeforeInsert.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addBeforeInsertListener(final ActionListener listener) {
    evtBeforeInsert.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeAfterInsertListener(final ActionListener listener) {
    evtAfterInsert.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addAfterInsertListener(final ActionListener listener) {
    evtAfterInsert.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeBeforeUpdateListener(final ActionListener listener) {
    evtBeforeUpdate.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addBeforeUpdateListener(final ActionListener listener) {
    evtBeforeUpdate.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeAfterUpdateListener(final ActionListener listener) {
    evtAfterUpdate.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addAfterUpdateListener(final ActionListener listener) {
    evtAfterUpdate.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void addBeforeDeleteListener(final ActionListener listener) {
    evtBeforeDelete.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeBeforeDeleteListener(final ActionListener listener) {
    evtBeforeDelete.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeAfterDeleteListener(final ActionListener listener) {
    evtAfterDelete.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addAfterDeleteListener(final ActionListener listener) {
    evtAfterDelete.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeEntitiesChangedListener(final ActionListener listener) {
    evtEntitiesChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addEntitiesChangedListener(final ActionListener listener) {
    evtEntitiesChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void addBeforeRefreshListener(final ActionListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeBeforeRefreshListener(final ActionListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addAfterRefreshListener(final ActionListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeAfterRefreshListener(final ActionListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  /**
   * Inserts the given entities from the database
   * @param entities the entities to insert
   * @return a list containing the primary keys of the inserted entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DatabaseException, CancelException {
    return connectionProvider.getConnection().insert(entities);
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the operation is cancelled
   */
  protected List<Entity> doUpdate(final List<Entity> entities) throws DatabaseException, CancelException {
    return connectionProvider.getConnection().update(entities);
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected void doDelete(final List<Entity> entities) throws DatabaseException, CancelException {
    connectionProvider.getConnection().delete(EntityUtil.getPrimaryKeys(entities));
  }

  /**
   * @return the actual {@link Entity} instance being edited
   */
  protected final Entity getEntity() {
    return (Entity) getValueMap();
  }

  private void bindEventsInternal() {
    evtAfterDelete.addListener(evtEntitiesChanged);
    evtAfterInsert.addListener(evtEntitiesChanged);
    evtAfterUpdate.addListener(evtEntitiesChanged);
    addValueMapSetListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        stEntityNull.setActive(getEntity().isPrimaryKeyNull());
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      for (final Property property : Entities.getProperties(entityID).values()) {
        addValueSetListener(property.getPropertyID(), new StatusMessageListener());
      }
      getEntity().addValueListener(new StatusMessageListener());
    }
  }

  private boolean containsLookupModel(final Property.ForeignKeyProperty property) {
    return entityLookupModels.containsKey(property);
  }

  private boolean containsComboBoxModel(final Property property) {
    return propertyComboBoxModels.containsKey(property);
  }

  private static String getValueChangeDebugString(final ValueChangeEvent<String, Object> event) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (event.getSource() instanceof Entity) {
      stringBuilder.append("[entity] ");
    }
    else {
      stringBuilder.append(event.isModelChange() ? "[model] " : "[ui] ");
    }
    final Entity valueOwner = (Entity) event.getValueOwner();
    final Property property = Entities.getProperty(valueOwner.getEntityID(), event.getKey());
    stringBuilder.append(valueOwner.getEntityID()).append(" : ").append(property).append(
            property.hasParentProperty() ? " [fk]" : "").append("; ");
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
    public Collection<Object> getValues() {
      try {
        return connectionProvider.getConnection().selectPropertyValues(entityID, propertyID, true);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class StatusMessageListener extends ValueChangeListener<String, Object> {
    /** {@inheritDoc} */
    @Override
    protected void valueChanged(final ValueChangeEvent<String, Object> event) {
      final String msg = getValueChangeDebugString(event);
      System.out.println(msg);
      LOG.debug(msg);
    }
  }
}