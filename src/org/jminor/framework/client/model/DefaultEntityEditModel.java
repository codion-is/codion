/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
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
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A default EntityEditModel implementation
 */
public class DefaultEntityEditModel extends AbstractValueChangeMapEditModel<String, Object> implements EntityEditModel {

  protected static final Logger LOG = Util.getLogger(DefaultEntityEditModel.class);

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
   * Indicates whether the model is active and ready to receive input
   */
  private final State stActive = States.state(Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE));

  /**
   * The ID of the entity this edit model is based on
   */
  private final String entityID;

  /**
   * The EntityDbProvider instance to use
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds the ComboBoxModels used by this EntityModel, those that implement Refreshable
   * are refreshed when refreshComboBoxModels() is called
   * @see org.jminor.common.model.Refreshable
   */
  private final Map<Property, FilteredComboBoxModel> propertyComboBoxModels = new HashMap<Property, FilteredComboBoxModel>();

  /**
   * The mechanism for restricting a single active EntityEditModel at a time
   */
  private static final State.StateGroup ACTIVE_STATE_GROUP = States.stateGroup();

  private boolean readOnly;

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance
   */
  public DefaultEntityEditModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, new DefaultEntityValidator(entityID, dbProvider));
  }

  /**
   * Instantiates a new EntityEditModel based on the entity identified by <code>entityID</code>.
   * @param entityID the ID of the entity to base this EntityEditModel on
   * @param dbProvider the EntityDbProvider instance
   * @param validator the validator to use
   */
  public DefaultEntityEditModel(final String entityID, final EntityDbProvider dbProvider, final EntityValidator validator) {
    super(Entities.entityInstance(entityID), validator);
    Util.rejectNullValue(dbProvider, "dbProvider");
    if (!Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE)) {
      ACTIVE_STATE_GROUP.addState(stActive);
    }
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.readOnly = Entities.isReadOnly(entityID);
    setValueMap(null);
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  public Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property.getPropertyID()) : property.getDefaultValue();
  }

  /** {@inheritDoc} */
  public boolean persistValueOnClear(final Property property) {
    return property instanceof Property.ForeignKeyProperty
            && Configuration.getBooleanValue(Configuration.PERSIST_FOREIGN_KEY_VALUES);
  }

  /** {@inheritDoc} */
  public FilteredComboBoxModel createPropertyComboBoxModel(final Property.ColumnProperty property, final EventObserver refreshEvent,
                                                           final String nullValueString) {
    return new DefaultPropertyComboBoxModel(entityID, dbProvider, property, nullValueString, refreshEvent);
  }

  /** {@inheritDoc} */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(), dbProvider);
    model.setNullValueString(getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID()) ?
            (String) Configuration.getValue(Configuration.DEFAULT_COMBO_BOX_NULL_VALUE_ITEM) : null);

    return model;
  }

  /** {@inheritDoc} */
  public EntityLookupModel createEntityLookupModel(final String entityID,
                                                   final List<Property.ColumnProperty> lookupProperties,
                                                   final Criteria additionalSearchCriteria) {
    final EntityLookupModel model = new DefaultEntityLookupModel(entityID, dbProvider, lookupProperties);
    model.setAdditionalLookupCriteria(additionalSearchCriteria);

    return model;
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
  public final boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  /** {@inheritDoc} */
  public final EntityEditModel setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
    return this;
  }

  /** {@inheritDoc} */
  public final StateObserver getAllowInsertState() {
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
  public final StateObserver getAllowUpdateState() {
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
  public final StateObserver getAllowDeleteState() {
    return stAllowDelete.getObserver();
  }

  /** {@inheritDoc} */
  public final StateObserver getEntityNullState() {
    return stEntityNull.getObserver();
  }

  /** {@inheritDoc} */
  public final StateObserver getActiveState() {
    return stActive.getObserver();
  }

  /** {@inheritDoc} */
  public final void setActive(final boolean active) {
    stActive.setActive(active);
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
  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /** {@inheritDoc} */
  public final Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    return (Entity) getValue(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  public final Entity getEntityCopy() {
    return getEntityCopy(true);
  }

  /** {@inheritDoc} */
  public final Entity getEntityCopy(final boolean includePrimaryKeyValues) {
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includePrimaryKeyValues) {
      copy.getPrimaryKey().clear();
    }

    return copy;
  }

  /** {@inheritDoc} */
  public final boolean isEntityNew() {
    final Entity.Key key = ((Entity) getValueMap()).getPrimaryKey();
    final Entity.Key originalkey = ((Entity) getValueMap()).getOriginalPrimaryKey();
    return key.isNull() || originalkey.isNull();
  }

  /** {@inheritDoc} */
  public final void insert() throws CancelException, DbException, ValidationException {
    insert(Arrays.asList(getEntityCopy(!Entities.isPrimaryKeyAutoGenerated(entityID))));
  }

  /** {@inheritDoc} */
  public final void insert(final List<Entity> entities) throws CancelException, DbException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, inserting is not allowed!");
    }
    if (!isInsertAllowed()) {
      throw new RuntimeException("This model does not allow inserting!");
    }

    LOG.debug(toString() + " - insert " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeInsert.fire();
    ((EntityValidator) getValidator()).validate(entities, EntityValidator.INSERT);

    final List<Entity.Key> primaryKeys = EntityUtil.copyKeys(doInsert(entities));
    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));
  }

  /** {@inheritDoc} */
  public final void update() throws CancelException, DbException, ValidationException {
    update(Arrays.asList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  public final void update(final List<Entity> entities) throws DbException, CancelException, ValidationException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, updating is not allowed!");
    }
    if (!isUpdateAllowed()) {
      throw new RuntimeException("This model does not allow updating!");
    }

    LOG.debug(toString() + " - update " + Util.getCollectionContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.isEmpty()) {
      return;
    }

    evtBeforeUpdate.fire();
    ((EntityValidator) getValidator()).validate(modifiedEntities, EntityValidator.UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    final int index = updatedEntities.indexOf(getEntity());
    if (index >= 0) {
      setEntity(updatedEntities.get(index));
    }

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities, EntityUtil.isPrimaryKeyModified(modifiedEntities)));
  }

  /** {@inheritDoc} */
  public final void delete() throws DbException, CancelException {
    delete(Arrays.asList(getEntityCopy()));
  }

  /** {@inheritDoc} */
  public final void delete(final List<Entity> entities) throws DbException, CancelException {
    Util.rejectNullValue(entities, "entities");
    if (entities.isEmpty()) {
      return;
    }
    if (readOnly) {
      throw new RuntimeException("This is a read-only model, deleting is not allowed!");
    }
    if (!isDeleteAllowed()) {
      throw new RuntimeException("This model does not allow deleting!");
    }

    LOG.debug(toString() + " - delete " + Util.getCollectionContentsAsString(entities, false));

    evtBeforeDelete.fire();

    doDelete(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));
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
  public final FilteredComboBoxModel getPropertyComboBoxModel(final Property.ColumnProperty property) {
    Util.rejectNullValue(property, "property");
    final FilteredComboBoxModel comboBoxModel = propertyComboBoxModels.get(property);
    if (comboBoxModel == null) {
      throw new RuntimeException("No PropertyComboBoxModel has been initialized for property: " + property);
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
      setComboBoxModel(property, comboBoxModel);
      comboBoxModel.refresh();
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = Entities.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return getEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel getEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      throw new RuntimeException("No EntityComboBoxModel has been initialized for property: " + foreignKeyProperty);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel initializeEntityComboBoxModel(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = Entities.getProperty(entityID, propertyID);
    if (!(property instanceof Property.ForeignKeyProperty)) {
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.ForeignKeyProperty");
    }

    return initializeEntityComboBoxModel((Property.ForeignKeyProperty) property);
  }

  /** {@inheritDoc} */
  public final EntityComboBoxModel initializeEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) propertyComboBoxModels.get(foreignKeyProperty);
    if (comboBoxModel == null) {
      comboBoxModel = createEntityComboBoxModel(foreignKeyProperty);
      setComboBoxModel(foreignKeyProperty, comboBoxModel);
    }

    return comboBoxModel;
  }

  /** {@inheritDoc} */
  public final boolean containsComboBoxModel(final String propertyID) {
    return containsComboBoxModel(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  public final boolean containsComboBoxModel(final Property property) {
    Util.rejectNullValue(property, "property");
    return propertyComboBoxModels.containsKey(property);
  }

  /** {@inheritDoc} */
  public final Entity getDefaultValueMap() {
    final Entity defaultEntity = Entities.entityInstance(entityID);
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
    return new PropertyValueProvider(dbProvider, entityID, property.getPropertyID());
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
    evtAfterInsert.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addEntitiesChangedListener(final ActionListener listener) {
    evtAfterInsert.addListener(listener);
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
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DbException, CancelException {
    try {
      return dbProvider.getEntityDb().insert(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list containing the updated entities
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is cancelled
   */
  protected List<Entity> doUpdate(final List<Entity> entities) throws DbException, CancelException {
    try {
      return dbProvider.getEntityDb().update(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws DbException in case of a database exception
   * @throws CancelException in case the operation is canceled
   */
  protected void doDelete(final List<Entity> entities) throws DbException, CancelException {
    try {
      dbProvider.getEntityDb().delete(EntityUtil.getPrimaryKeys(entities));
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the Entity instance being edited
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
        stEntityNull.setActive(getEntity().isNull());
      }
    });
    if (Configuration.getBooleanValue(Configuration.PROPERTY_DEBUG_OUTPUT)) {
      getEntity().addValueListener(new StatusMessageListener());
    }
  }

  /**
   * Sets the ComboBoxModel to be associated with the given property
   * @param property the property
   * @param model the ComboBoxModel
   * @throws RuntimeException in case the ComboBoxModel has already been set for this property
   */
  private void setComboBoxModel(final Property property, final FilteredComboBoxModel model) {
    if (propertyComboBoxModels.containsKey(property)) {
      throw new RuntimeException("ComboBoxModel already associated with property: " + property);
    }

    propertyComboBoxModels.put(property, model);
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

    private final EntityDbProvider dbProvider;
    private final String entityID;
    private final String propertyID;

    private PropertyValueProvider(final EntityDbProvider dbProvider, final String entityID, final String propertyID) {
      this.dbProvider = dbProvider;
      this.entityID = entityID;
      this.propertyID = propertyID;
    }

    /** {@inheritDoc} */
    public Collection<Object> getValues() {
      try {
        return dbProvider.getEntityDb().selectPropertyValues(entityID, propertyID, true);
      }
      catch (Exception e) {
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