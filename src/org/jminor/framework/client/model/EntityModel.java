/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.IRefreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.combobox.PropertyComboBoxModel;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyChangeEvent;
import org.jminor.framework.model.PropertyListener;

import org.apache.log4j.Logger;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model class with basic functionality for creating, editing and deleting of objects from a database
 */
public class EntityModel implements IRefreshable {

  protected static final Logger log = Util.getLogger(EntityModel.class);

  /**
   * Code for the insert action
   */
  public static final int INSERT = 1;

  /**
   * Code for the update action
   */
  public static final int UPDATE = 2;

  /**
   * Fired before an insert is performed
   */
  public final Event evtBeforeInsert = new Event("EntityModel.evtBeforeInsert");

  /**
   * Fired when an Entity has been inserted
   */
  public final Event evtEntityInserted = new Event("EntityModel.evtEntityInserterd");

  /**
   * Fired before an update is performed
   */
  public final Event evtBeforeUpdate = new Event("EntityModel.evtBeforeUpdate");

  /**
   * Fired when an Entity has been updated
   */
  public final Event evtEntitiesUpdated = new Event("EntityModel.evtEntitiesUpdated");

  /**
   * Fired before a delete is performed
   */
  public final Event evtBeforeDelete = new Event("EntityModel.evtBeforeDelete");

  /**
   * Fired when an Entity has been deleted
   */
  public final Event evtEntityDeleted = new Event("EntityModel.evtEntityDeleted");

  /**
   * Fired when an entity is deleted, inserted or updated
   */
  public final Event evtEntitiesChanged = new Event("EntityModel.evtEntitiesChanged");

  /**
   * Fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public final Event evtRefreshDone = new Event("EntityModel.evtRefreshDone");

  /**
   * Fired when the model is about to be refreshed
   */
  public final Event evtRefreshStarted = new Event("EntityModel.evtRefreshStarted");

  /**
   * Fired when the model has been cleared
   */
  public final Event evtModelCleared = new Event("EntityModel.evtModelCleared");

  /**
   * Fired when the active entity is changed
   */
  public final Event evtActiveEntityChanged = new Event("EntityModel.evtActiveEntityChanged");

  /**
   * Indicates whether the model is active
   */
  public final State stActive = new State("EntityModel.stActive",
          (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.ALL_MODELS_ENABLED));

  /**
   * Active when a non-null entity is active
   */
  public final State stEntityActive = new State("EntityModel.stEntityActive");

  /**
   * When active the detail models are updated and filtered according to the selected master entity, if any
   */
  public final Event evtLinkedDetailModelsChanged = new Event("EntityModel.evtLinkedDetailModelsChanged");

  /**
   * Selection in this model triggers a filtering in all detail models
   * if this state is active
   */
  private final State stSelectionFiltersDetail = new State("EntityModel.stSelectionFiltersDetail", true);

  /**
   * Refresh of this model triggers a refresh in all detail models if this state is active
   */
  private final State stCascadeRefresh = new State("EntityModel.stCascadeRefresh", false);

  /**
   * The table model
   */
  private final EntityTableModel tableModel;

  /**
   * The IEntityDb connection provider
   */
  private final IEntityDbProvider dbConnectionProvider;

  /**
   * The currently selected entity
   */
  private final Entity activeEntity;

  /**
   * A caption describing this EntityModel
   */
  private final String caption;

  /**
   * The ID of the Entity this EntityModel represents
   */
  private final String entityID;

  /**
   * Holds the ComboBoxModels used by this EntityModel, those that implement IRefreshable
   * are refreshed when refreshComboBoxModels() is called
   * @see org.jminor.common.model.IRefreshable
   */
  private final Map<Property, ComboBoxModel> propertyComboBoxModels;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final List<? extends EntityModel> detailModels;

  /**
   * Holds detail models that should be updated according to the selected entity
   */
  private final List<EntityModel> linkedDetailModels = new ArrayList<EntityModel>();

  /**
   * Holds the primary key properties of the last insert batch
   */
  private final List<EntityKey> lastInsertedEntityPrimaryKeys = new ArrayList<EntityKey>();

  /**
   * Holds the updated entities from the last update batch
   */
  private final List<Entity> lastUpdatedEntities = new ArrayList<Entity>();

  /**
   * Holds the Entities from the last delete batch
   */
  private final List<Entity> lastDeletedEntities = new ArrayList<Entity>();

  /**
   * True while the model is refreshing itself
   */
  private boolean isRefreshing = false;

  /**
   * Holds events signaling changes made to the active entity via the ui
   */
  private final Map<Property, Event> uiChangeEventMap = new HashMap<Property, Event>();

  /**
   * Holds events signaling changes made to the active entity via the model
   */
  private final Map<Property, Event> modelChangeEventMap = new HashMap<Property, Event>();

  /**
   * Holds events signaling changes made to the active entity, via the model or ui
   */
  private final Map<Property, Event> changeEventMap = new HashMap<Property, Event>();

  /**
   * If true, then the modification of a record triggers a select for update
   */
  private boolean strictEditingEnabled = (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.USE_STRICT_EDIT_MODE);

  /**
   * Is true while the active record is in a locked state (selected for update)
   */
  private boolean strictEditLockEnabled = false;

  /**
   * The mechanism for restricting a single active EntityModel at a time
   */
  private static final State.StateGroup activeStateGroup = new State.StateGroup();

  /**
   * Initiates a new EntityModel
   * @param caption a caption describing this EntityModel
   * @param dbProvider a IEntityDbProvider
   * @param entityID the ID of the Entity this EntityModel represents
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public EntityModel(final String caption, final IEntityDbProvider dbProvider,
                     final String entityID) throws UserException {
    this(caption, dbProvider, entityID, true);
  }

  /**
   * Initiates a new EntityModel
   * @param caption a caption describing this EntityModel
   * @param dbProvider a IEntityDbProvider
   * @param entityID the ID of the Entity this EntityModel represents
   * @param includeTableModel true if this EntityModel should include a table model
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public EntityModel(final String caption, final IEntityDbProvider dbProvider,
                     final String entityID, final boolean includeTableModel) throws UserException {
    if (!(Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.ALL_MODELS_ENABLED))
      activeStateGroup.addState(stActive);//todo potential memory leak
    this.caption = caption;
    this.dbConnectionProvider = dbProvider;
    this.entityID = entityID;
    this.propertyComboBoxModels = initializeEntityComboBoxModels();
    this.tableModel = includeTableModel ? initializeTableModel() : null;
    this.activeEntity = getDefaultValue();
    this.detailModels = initializeDetailModels();
    this.activeEntity.setFirePropertyChangeEvents(true);
    initializeAssociatedModels();
    bindEvents();
    bindTableModelEvents();
  }

  /**
   * @return the class of the Entity this model represents
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * Sets the strict editing mode, if true the model locks the active record while it is being edited
   * @param strictEditMode the strict editing mode
   * @throws UserException in case of an exception
   */
  public void setStrictEditMode(final boolean strictEditMode) throws UserException {
    if (!strictEditMode)
      setActiveEntityWriteLock(false);

    this.strictEditingEnabled = strictEditMode;
  }

  /**
   * @return a String represention of this EntityModel,
   * returns the model id by default
   */
  public String toString() {
    return getCaption();
  }

  /**
   * @return the database connection provider
   */
  public IEntityDbProvider getDbConnectionProvider() {
    return dbConnectionProvider;
  }

  /**
   * @return the database connection
   * @throws UserException in case of an exception
   */
  public IEntityDb getEntityDb() throws UserException {
    return getDbConnectionProvider().getEntityDb();
  }

  /**
   * @return the id of this model
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return Value for property 'readOnly'.
   */
  public boolean isReadOnly() {
    return EntityRepository.get().isReadOnly(entityID);
  }

  /**
   * @return true if the cascade refresh state is active
   */
  public boolean getCascadeRefresh() {
    return stCascadeRefresh.isActive();
  }

  /**
   * Sets the cascade refresh state
   * @param value the new casecade refresh value
   */
  public void setCascadeRefresh(final boolean value) {
    for (final EntityModel detailModel : detailModels)
      detailModel.setCascadeRefresh(value);

    stCascadeRefresh.setActive(value);
  }

  /**
   * @return true if the selection filters detail state is active
   */
  public boolean getSelectionFiltersDetail() {
    return stSelectionFiltersDetail.isActive();
  }

  /**
   * Sets the selection filters detail state
   * @param value the new selection filters detail value
   * @see #masterSelectionChanged
   * @see #masterSelectionChanged
   * @see org.jminor.framework.client.model.EntityTableModel#filterByReference(java.util.List, String)
   */
  public void setSelectionFiltersDetail(final boolean value) {
    for (final EntityModel detailModel : detailModels)
      detailModel.setSelectionFiltersDetail(value);

    getTableModel().getSelectionModel().clearSelection();
    stSelectionFiltersDetail.setActive(value);
  }

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  public boolean getAllowMultipleUpdate() {
    return true;
  }

  /**
   * @return true if this model allows records to be inserted
   */
  public boolean allowInsert() {
    return true;
  }

  /**
   * @return true if this model allows records to be updated
   */
  public boolean allowUpdate() {
    return true;
  }

  /**
   * @return true if this model allows records to be deleted
   */
  public boolean allowDelete() {
    return true;
  }

  /**
   * @return the EntityTableModel, null if none is specified
   */
  public EntityTableModel getTableModel() {
    return tableModel;
  }

  /**
   * @param modelClass the detail model class
   * @return true if this model contains a detail model of the given class
   */
  public boolean containsDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels)
      if (detailModel.getClass().equals(modelClass))
        return true;

    return false;
  }

  /**
   * @return the detail models this model contains
   */
  public List<? extends EntityModel> getDetailModels() {
    return detailModels;
  }

  /**
   * @return the linked detail model or the first detail model if none is linked,
   * returns null in case this model contains no detail models
   */
  public EntityModel getLinkedDetailModel() {
    if (detailModels.size() == 0)
      return null;

    return linkedDetailModels.size() > 0 ? linkedDetailModels.get(0) : detailModels.get(0);
  }

  /**
   * Sets the currently linked detail model, that is, the one that should be
   * updated according to the selected item
   * @param detailModel the detail model to link
   */
  public void setLinkedDetailModel(final EntityModel detailModel) {
    setLinkedDetailModels(detailModel == null ? null : Arrays.asList(detailModel));
  }

  /**
   * Sets the currently linked detail models
   * @param detailModels the detail models to link
   */
  public void setLinkedDetailModels(final List<EntityModel> detailModels) {
    linkedDetailModels.clear();
    if (detailModels != null)
      linkedDetailModels.addAll(detailModels);

    evtLinkedDetailModelsChanged.fire();
  }

  /**
   * @return a list containing the detail models that are currently linked to this model
   */
  public List<EntityModel> getLinkedDetailModels() {
    return linkedDetailModels;
  }

  /**
   * Returns the detail model for the given Class
   * @param entityModelClass the Class of the Entity for which to find the detail model
   * @return the detail model for the given Entity Class, null if none is found
   */
  public EntityModel getDetailModel(final Class<?> entityModelClass) {
    for (final EntityModel detailModel : detailModels)
      if (detailModel.getClass().equals(entityModelClass))
        return detailModel;

    return null;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the property <code>propertyID</code>,
   * if the EntityComboBoxModel has not been initialized in <code>initializeEntityComboBoxModels</code> one
   * is automatically created
   */
  public EntityComboBoxModel getEntityComboBoxModel(final String propertyID) {
    final Property property = EntityRepository.get().getProperty(getEntityID(), propertyID);
    if (!(property instanceof Property.EntityProperty))
      throw new IllegalArgumentException("EntityComboBoxModels are only available for Property.EntityProperty");

    return getEntityComboBoxModel((Property.EntityProperty) property);
  }

  /**
   * @param property the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the <code>property</code>,
   * if the EntityComboBoxModel has not been initialized in <code>initializeEntityComboBoxModels</code> one
   * is automatically created
   *///todo parameter for autoCreate or rename methods?
  public EntityComboBoxModel getEntityComboBoxModel(final Property.EntityProperty property) {
    ComboBoxModel ret = getComboBoxModel(property);
    if (ret == null)
      setComboBoxModel(property, ret = new EntityComboBoxModel(getDbConnectionProvider(), property.referenceEntityID));

    return (EntityComboBoxModel) ret;
  }

  /**
   * Fetches the ComboBoxModel associated with this property, these must be initialized beforehand,
   * by overriding initializeEntityComboBoxModels()
   * @param propertyID the property identifier
   * @return the ComboBoxModel associated with this property
   * @see #initializeEntityComboBoxModels()
   * @see #initializeEntityComboBoxModels(org.jminor.framework.client.model.combobox.EntityComboBoxModel[])()
   */
  public ComboBoxModel getComboBoxModel(final String propertyID) {
    return getComboBoxModel(EntityRepository.get().getProperty(getEntityID(), propertyID));
  }

  /**
   * Fetches the ComboBoxModel associated with this property,
   * returns null if none is associated with the given property
   * @param property the property
   * @return the ComboBoxModel associated with this property, null if none is available
   */
  public ComboBoxModel getComboBoxModel(final Property property) {
    return propertyComboBoxModels.get(property);
  }

  /**
   * Clears the model by setting the active entity to null
   * @see #evtModelCleared
   */
  public final void clear() {
    setActive(null);
    evtModelCleared.fire();
  }

  /**
   * @return true if the active entity is null
   * @see org.jminor.framework.model.Entity#isNull()
   */
  public boolean isActiveEntityNull() {
    return activeEntity.isNull();
  }

  /**
   * @return a copy of the active entity
   * @see org.jminor.framework.model.Entity#getCopy()
   */
  public Entity getActiveEntityCopy() {
    return activeEntity.getCopy();
  }

  /**
   * @return true if the active entity has been modified
   * @see org.jminor.framework.model.Entity#isModified()
   */
  public boolean isActiveEntityModified() {
    return activeEntity.isModified();
  }

  /**
   * Default behaviour is returning a copy of the active entity
   * This method should return an empty List instead of null.
   * @return the entities to use when insert is triggered
   * @see #insert()
   */
  public List<Entity> getEntitiesForInsert() {
    return Arrays.asList(getActiveEntityCopy());
  }

  /**
   * This method should return an empty List instead of null.
   * Default behaviour is returning a copy of the active entity
   * @return the entities to use when update is triggered
   * @see #update()
   */
  public List<Entity> getEntitiesForUpdate() {
    return Arrays.asList(getActiveEntityCopy());
  }

  /**
   * Returns the entities to use when delete is triggered.
   * Default behaviour is returning the active entity or the
   * selected entities in case the active entity is null.
   * This method should return an empty List instead of null.
   * @return the entities to use when delete is triggered
   * @see #delete()
   */
  public List<Entity> getEntitiesForDelete() {
    return isActiveEntityNull() ? (getTableModel() != null ? getTableModel().getSelectedEntities()
            : new ArrayList<Entity>()) :  Arrays.asList(getActiveEntityCopy());
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of property <code>propertyID</code> is changed by the UI
   */
  public Event getPropertyUIChangeEvent(final String propertyID) {
    return getPropertyUIChangeEvent(EntityRepository.get().getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> is changed by the UI
   */
  public Event getPropertyUIChangeEvent(final Property property) {
    if (uiChangeEventMap.containsKey(property))
      return uiChangeEventMap.get(property);

    final Event ret = new Event("EntityModel.propertyUIChangeEvent " + property);
    uiChangeEventMap.put(property, ret);

    return ret;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of property <code>propertyID</code> is changed by the model
   */
  public Event getPropertyModelChangeEvent(final String propertyID) {
    return getPropertyModelChangeEvent(EntityRepository.get().getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> is changed by the model
   */
  public Event getPropertyModelChangeEvent(final Property property) {
    if (modelChangeEventMap.containsKey(property))
      return modelChangeEventMap.get(property);

    final Event ret = new Event("EntityModel.propertyModelChangeEvent " + property);
    modelChangeEventMap.put(property, ret);

    return ret;
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the event
   * @return an Event object which fires when the value of property <code>propertyID</code> is changed
   */
  public Event getPropertyChangeEvent(final String propertyID) {
    return getPropertyChangeEvent(EntityRepository.get().getProperty(getEntityID(), propertyID));
  }

  /**
   * @param property the property for which to retrieve the event
   * @return an Event object which fires when the value of <code>property</code> is changed
   */
  public Event getPropertyChangeEvent(final Property property) {
    if (changeEventMap.containsKey(property))
      return changeEventMap.get(property);

    final Event ret = new Event("EntityModel.propertyChangeEvent " + property);
    activeEntity.getPropertyChangeEvent().addListener(new PropertyListener() {
      protected void propertyChanged(final PropertyChangeEvent e) {
        if (e.getProperty().equals(property))
          ret.fire(e);
      }
    });
    changeEventMap.put(property, ret);

    return ret;
  }

  /**
   * @return the state which indicates the modified state of the active entity
   * @see org.jminor.framework.model.Entity#getModifiedState()
   */
  public State getEntityModifiedState() {
    return activeEntity.getModifiedState();
  }

  /**
   * Sets the active entity
   * @param entity the entity to set as active
   * @see #evtActiveEntityChanged
   */
  public final void setActive(final Entity entity) {
    if (entity != null && activeEntity.propertyValuesEqual(entity))
      return;
    if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT) && entity != null)
      EntityUtil.printPropertyValues(entity);
    activeEntity.setAs(entity == null ? getDefaultValue() : entity);

    stEntityActive.setActive(!activeEntity.isNull());

    evtActiveEntityChanged.fire();
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>,
   * basic type validation is performed.
   * @param propertyID the ID of the property to update
   * @param value the new value
   */
  public final void uiSetValue(final String propertyID, final Object value) {
    uiSetValue(propertyID, value, true);
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>
   * @param propertyID the ID of the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   */
  public final void uiSetValue(final String propertyID, final Object value, final boolean validate) {
    uiSetValue(EntityRepository.get().getProperty(getEntityID(), propertyID), value, validate);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   */
  public final void uiSetValue(final Property property, final Object value) {
    uiSetValue(property, value, true);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>.
   * @param property the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   */
  public final void uiSetValue(final Property property, final Object value, final boolean validate) {
    uiSetValue(property, value, validate, true);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   * @param notify if true then a property change event is fired
   */
  public final void uiSetValue(final Property property, final Object value, final boolean validate,
                               final boolean notify) {
    setValue(property, value, validate, false, notify);
  }

  /**
   * @param propertyID the property identifier
   * @return true if the value of the given property is null
   */
  public final boolean isValueNull(final String propertyID) {
    return activeEntity.isValueNull(propertyID);
  }

  public final Object getValue(final Property property) {
    return getValue(property.propertyID);
  }

  public final Object getValue(final String propertyID) {
    return activeEntity.getValue(propertyID);
  }

  public final Entity getEntityValue(final String propertyID) {
    return activeEntity.getEntityValue(propertyID);
  }

  public final Entity getEntityValue(final Property property) {
    return getEntityValue(property.propertyID);
  }

  /**
   * Performes a insert, using the entities returned by getEntitiesForInsert()
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #getEntitiesForInsert()
   */
  public final void insert() throws UserException, UserCancelException, DbException {
    insert(getEntitiesForInsert());
  }

  /**
   * Performs an insert on the given entities
   * @param entities the entities to insert
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #evtBeforeInsert
   * @see #evtEntityInserted
   */
  public final void insert(final List<Entity> entities) throws UserException, DbException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, inserting is not allowed!");
    if (!allowInsert())
      throw new UserException("This is model does not allow inserting!");

    log.debug(caption + " - insert "+ Util.getListContents(entities, false));

    evtBeforeInsert.fire();
    validateData(entities, INSERT);
    lastInsertedEntityPrimaryKeys.clear();
    lastInsertedEntityPrimaryKeys.addAll(EntityKey.copyEntityKeys(doInsert(entities)));

    evtEntityInserted.fire();
    refreshDetailModelsAfterInsertOrUpdate();
  }

  /**
   * Performes a update, using the entities returned by getEntitiesForUpdate()
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #getEntitiesForUpdate()
   */
  public final void update() throws UserException, UserCancelException, DbException {
    update(getEntitiesForUpdate());
  }

  /**
   * Updates the given Entities
   * @param entities the Entities to update
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #evtBeforeUpdate
   * @see #evtEntitiesUpdated
   */
  //todo known issue, when updating primary key properties the table model state is not exactly fresh afterwards, example: petstore-ItemTags, proposed solution: force refresh in case primary keys were involved in the update
  public final void update(final List<Entity> entities) throws UserException, DbException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, updating is not allowed!");
    if (!getAllowMultipleUpdate() && entities.size() > 1)
      throw new UserException("Update of multiple entities is not allowed!");
    if (!allowUpdate())
      throw new UserException("This is model does not allow updating!");

    log.debug(caption + " - update " + Util.getListContents(entities, false));

    evtBeforeUpdate.fire();
    validateData(entities, UPDATE);
    lastUpdatedEntities.clear();
    for (final Entity entity : doUpdate(entities))
      lastUpdatedEntities.add(entity);

    evtEntitiesUpdated.fire();
    refreshDetailModelsAfterInsertOrUpdate();
  }

  /**
   * Deletes the entities returned by getEntitiesForDelete()
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #getEntitiesForDelete()
   * @see #evtBeforeDelete
   * @see #evtEntityDeleted
   */
  public final void delete() throws DbException, UserException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, deleting is not allowed!");
    if (!allowDelete())
      throw new UserException("This is model does not allow deleting!");

    final List<Entity> entities = getEntitiesForDelete();
    log.debug(caption + " - delete " + Util.getListContents(entities, false));

    evtBeforeDelete.fire();
    getTableModel().getSelectionModel().clearSelection();
    lastDeletedEntities.clear();
    doDelete(entities);
    for (final Entity entity : entities)
      lastDeletedEntities.add(entity.getCopy());

    evtEntityDeleted.fire();
    refreshDetailModelsAfterDelete();
  }

  /**
   * NB does not prevent calls to insert()
   * @return the state used to determine if inserting should be enabled
   */
  public State getAllowInsertState() {
    return new State("EntityModel.stAllowInsert", allowInsert());
  }

  /**
   * NB does not prevent calls to update()
   * @return the state used to determine if updating should be enabled
   */
  public State getAllowUpdateState() {
    return new State("EntityModel.stAllowUpdate", allowUpdate());
  }

  /**
   * NB does not prevent calls to delete()
   * @return the state used to determine if deleting should be enabled
   */
  public State getAllowDeleteState() {
    return new State("EntityModel.stAllowDelete", allowDelete());
  }

  /**
   * @return the entities in the last update batch,
   * if no update has been performed an empty list is returned
   */
  public List<Entity> getLastUpdatedEntities() {
    return lastUpdatedEntities;
  }

  /**
   * @return the primary keys of the entities in the last insert batch
   * if no insert has been performed null is returned
   */
  public List<EntityKey> getLastInsertedEntityPrimaryKeys() {
    return lastInsertedEntityPrimaryKeys;
  }

  /**
   * @return the Entities from the last delete batch,
   * if no delete has been performed an empty list is returned
   */
  public List<Entity> getLastDeletedEntities() {
    return lastDeletedEntities;
  }

  /**
   * Refreshes this EntityModel
   * @throws UserException in case of a user exception
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   * @see #getCascadeRefresh
   */
  public void refresh() throws UserException {
    if (isRefreshing)
      return;

    try {
      log.trace(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      if (tableModel != null)
        tableModel.refresh();

      refreshEntityComboBoxModels();
      if (getCascadeRefresh())
        refreshDetailModels();

      updateDetailModelsByActiveEntity();
    }
    catch(UserException ue) {
      throw ue;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      log.trace(this + " done refreshing");
    }
  }

  /**
   * Refreshes the IRefreshable ComboBoxModels associated with this EntityModel
   * @throws UserException in case of an exception
   * @see org.jminor.common.model.IRefreshable
   */
  public void refreshComboBoxModels() throws UserException {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values())
      if (comboBoxModel instanceof IRefreshable)
        ((IRefreshable) comboBoxModel).refresh();
  }

  /**
   * Updates this EntityModel according to the given master Entity,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterValue the value of the selected master entity
   * @param masterEntityID the ID of the master entity, in case the value is null
   * @throws UserException in case of a problem
   */
  public void masterSelectionChanged(final Entity masterValue, final String masterEntityID) throws UserException {
    masterSelectionChanged((masterValue == null || masterValue.isNull())
            ? new ArrayList<Entity>(0) : Arrays.asList(masterValue), masterEntityID);
  }

  /**
   * Updates this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterValues the master entities
   * @param masterEntityID the ID of the master entity, in case the value is null
   * @throws UserException in case of a problem
   */
  public void masterSelectionChanged(final List<Entity> masterValues, final String masterEntityID) throws UserException {
    if (stSelectionFiltersDetail.isActive() && tableModel != null)
      tableModel.filterByReference(masterValues, masterEntityID);

    if (masterValues != null && masterValues.size() > 0) {
      for (final Property.EntityProperty property : EntityRepository.get().getEntityProperties(getEntityID(), masterEntityID))
        setValue(property, masterValues.get(0));
    }
  }

  /**
   * @param property the property for which to create the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.evtEntitiesChanged is used
   * @param nullValue the null value at the top of the list
   * @return a ComboBoxModel containing the distinct values found in the
   */
  public ComboBoxModel getPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                final Object nullValue) {
    if (propertyComboBoxModels.containsKey(property))
      return propertyComboBoxModels.get(property);

    final PropertyComboBoxModel ret =
            createPropertyComboBoxModel(getEntityID(), property, getDbConnectionProvider(),
                    refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValue);

    propertyComboBoxModels.put(property, ret);

    return ret;
  }

  public ComboBoxModel getColumnComboBoxModel(final Property property) {
    return getColumnComboBoxModel(property, null);
  }

  public ComboBoxModel getColumnComboBoxModel(final Property property, final Event refreshEvent) {
    return getPropertyComboBoxModel(property, refreshEvent, null);
  }

  public static List<Class<? extends EntityModel>> asList(final Class<? extends EntityModel>... classes) {
    return new ArrayList<Class<? extends EntityModel>>(Arrays.asList(classes));
  }

  /**
   * @return a List of EntityModels serving as detail models
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return new ArrayList<EntityModel>(0);
  }

  /**
   * Override this method to initialize any associated EntityModel before bindEvents() is called
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected void initializeAssociatedModels() throws UserException {}

  /**
   * @return a map of initialized EntityComboBoxModels associated with
   * their respective properties.
   * Use this method to provide combo box models with specific functionality.
   */
  protected Map<Property, ComboBoxModel> initializeEntityComboBoxModels() {
    return initializeEntityComboBoxModels(new EntityComboBoxModel[0]);
  }

  /**
   * Returns a Map, mapping the EntityComboBoxModels provided to their respective
   * properties according to the entityID.
   * This implementation is rather simplistic, since it simply maps the EntityComboBoxModel to
   * the first (random) Property.EntityProperty with the same entityID, if the underlying Entity
   * references the same Entity via more than one property, this method should be avoided.
   * @param comboBoxModels the EntityComboBoxModels to map to their respective properties
   * @return a Map of EntityComboBoxModels mapped to their respective properties
   */
  protected final Map<Property, ComboBoxModel> initializeEntityComboBoxModels(final EntityComboBoxModel... comboBoxModels) {
    final HashMap<Property, ComboBoxModel> ret = new HashMap<Property, ComboBoxModel>();
    if (comboBoxModels == null || comboBoxModels.length == 0)
      return ret;

    for (final EntityComboBoxModel comboBoxModel : comboBoxModels) {
      final List<Property.EntityProperty > properties =
              EntityRepository.get().getEntityProperties(getEntityID(), comboBoxModel.getEntityID());
      if (properties.size() > 0)
        ret.put(properties.get(0), comboBoxModel);//todo perhaps throw an exception?
      else
        throw new RuntimeException("Property not found for EntityComboBoxModel: " + comboBoxModel);
    }

    return ret;
  }

  /**
   * Use this method to provide a specific order of property init notifications, this comes in handy
   * when f.ex. combo box models are used to filter each other, which can result in a modified entity
   * after initialization
   * @param properties the properties of the entity with the given ID
   * @return a list of property names, in the order the property value initialization notifications should be handled
   */
  protected List<String> getPropertyNotificationOrder(final Collection<Property> properties) {
    final List<String> ret = new ArrayList<String>(properties.size());
    for (final Property property : properties)
      ret.add(property.propertyID);

    return ret;
  }

  /**
   * Validates the given Entity objects
   * For overriding
   * @param entities the entities to validate
   * @param action describes the action requiring validation, INSERT or UPDATE
   * @throws UserException in case of a user exception
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void validateData(final List<Entity> entities, final int action) throws UserException {}

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>,
   * basic type validation is performed. The value change is assumed to be triggered by the model.
   * @param propertyID the ID of the property to update
   * @param value the new value
   */
  protected final void setValue(final String propertyID, final Object value) {
    setValue(propertyID, value, true);
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>.
   * The value change is assumed to be triggered by the model.
   * @param propertyID the ID of the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   */
  protected final void setValue(final String propertyID, final Object value, final boolean validate) {
    setValue(propertyID, value, validate, true);
  }

  /**
   * Sets the value of the property with name <code>propertyID</code> in the active entity to <code>value</code>
   * @param propertyID the ID of the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   * @param isModelChange indicates whether the change is triggered by the model,
   * if false then the UI is assumed to be responsible for the value change
   */
  protected final void setValue(final String propertyID, final Object value, final boolean validate,
                                final boolean isModelChange) {
    setValue(EntityRepository.get().getProperty(getEntityID(), propertyID), value, validate, isModelChange);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   */
  protected final void setValue(final Property property, final Object value) {
    setValue(property, value, true);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>.
   * The value change is assumed to be triggered by the model.
   * @param property the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   */
  protected final void setValue(final Property property, final Object value, final boolean validate) {
    setValue(property, value, validate, true);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   * @param isModelChange indicates whether the change is triggered by the model,
   * if false then the UI is assumed to be responsible for the value change
   */
  protected final void setValue(final Property property, final Object value, final boolean validate,
                                final boolean isModelChange) {
    setValue(property, value, validate, isModelChange, true);
  }

  /**
   * Sets the value of <code>property</code> in the active entity to <code>value</code>
   * @param property the property to update
   * @param value the new value
   * @param validate if true basic type validation is performed
   * @param isModelChange indicates whether the change is triggered by the model,
   * if false then the UI is assumed to be responsible for the value change
   * @param notify if true then a property change event is fired
   */
  protected final void setValue(final Property property, final Object value, final boolean validate,
                                final boolean isModelChange, final boolean notify) {
    final Object oldValue = getValue(property);
    final Object newValue = doSetValue(property, value, validate);
    if (notify && !Util.equal(newValue, oldValue))
      notifyPropertyChanged(property, newValue, oldValue, isModelChange);
  }

  /**
   * If this method is overridden then calling super.getDefaultValue() would be proper
   * @return the default value for this EntitModel, it is set as active when no item is selected
   */
  protected Entity getDefaultValue() {
    final Entity ret = new Entity(getEntityID());
    //correctly represent the gui by setting the values from ComboBoxModels that should not be reset on clearStateData
    for (final Map.Entry<Property, ComboBoxModel> entry : propertyComboBoxModels.entrySet()) {
      final Property property = entry.getKey();
      if (!resetComboBoxModelOnClear(property)) {
        final ComboBoxModel boxModel = entry.getValue();
        Object value = boxModel.getSelectedItem();
        if (boxModel instanceof EntityComboBoxModel && ((EntityComboBoxModel) boxModel).isNullValueSelected())
          value = null;
        final Object currentValue = ret.getValue(property);
        if (!EntityUtil.equal(property.propertyType, value, currentValue))
          ret.setValue(property, value, false);
      }
    }

    return ret;
  }

  /**
   * Override to enable the reset of entity combo box models when the model is cleared.
   * By default this method returns the value of <code>FrameworkSettings.resetComboBoxModelsOnClear</code>.
   * @param property the property
   * @return true if the ComboBoxModel should be reset when the model is cleared
   * @see FrameworkSettings#RESET_COMBOBOXMODELS_ON_CLEAR
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected boolean resetComboBoxModelOnClear(final Property property) {
    return (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.RESET_COMBOBOXMODELS_ON_CLEAR);
  }

  protected List<EntityKey> doInsert(final List<Entity> entities) throws DbException, UserException, UserCancelException {
    try {
      return getEntityDb().insert(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected List<Entity> doUpdate(final List<Entity> entities) throws DbException, UserException, UserCancelException {
    try {
      return getEntityDb().update(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected void doDelete(final List<Entity> entities) throws DbException, UserException, UserCancelException {
    try {
      getEntityDb().delete(entities);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getDbConnectionProvider(), getEntityID());
  }

  /**
   * Override to add specific event bindings, remember to call super.bindEvents()
   */
  protected void bindEvents() {
    evtEntityDeleted.addListener(evtEntitiesChanged);
    evtEntityInserted.addListener(evtEntitiesChanged);
    evtEntitiesUpdated.addListener(evtEntitiesChanged);
    evtLinkedDetailModelsChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          updateDetailModelsByActiveEntity();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
    activeEntity.getModifiedState().evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          if (strictEditingEnabled && !isActiveEntityNull())
            setActiveEntityWriteLock(activeEntity.getModifiedState().isActive());
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
    //always release the write lock if the entity being edited is de-selected
    evtActiveEntityChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          if (strictEditingEnabled)
            setActiveEntityWriteLock(false);
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
    if (tableModel == null) {
      evtActiveEntityChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            updateDetailModelsByActiveEntity();
          }
          catch (UserException e1) {
            throw e1.getRuntimeException();
          }
        }
      });
    }
  }

  protected void bindTableModelEvents() {
    if (tableModel == null)
      return;

    tableModel.evtSelectionChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          updateDetailModelsByActiveEntity();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });

    evtEntityDeleted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tableModel.removeEntities(getLastDeletedEntities());
      }
    });

    evtEntityInserted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          tableModel.getSelectionModel().clearSelection();
          tableModel.addEntitiesByPrimaryKeys(getLastInsertedEntityPrimaryKeys(), true);
        }
        catch (UserException ue) {
          throw ue.getRuntimeException();
        }
        catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });

    evtEntitiesUpdated.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final List<Entity> updatedEntities = new ArrayList<Entity>(lastUpdatedEntities.size());
        for (final Entity entity : lastUpdatedEntities)
          if (entity.getEntityID().equals(getEntityID()))
            updatedEntities.add(entity);
        tableModel.replaceEntities(updatedEntities);
        tableModel.setSelectedEntities(updatedEntities);
      }
    });

    tableModel.evtSelectedIndexChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        setActive(tableModel.getSelectionModel().isSelectionEmpty() ? null : tableModel.getSelectedEntity());
      }
    });
  }

  /**
   * Removes the deleted entities from combobox models
   */
  protected void refreshDetailModelsAfterDelete() {
    final List<Entity> lastDeleted = getLastDeletedEntities();
    if (lastDeleted != null && lastDeleted.size() > 0) {
      for (final EntityModel detailModel : detailModels) {
        for (final Property.EntityProperty property :
                EntityRepository.get().getEntityProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityComboBoxModel comboModel =
                (EntityComboBoxModel) detailModel.propertyComboBoxModels.get(property);
          if (comboModel != null) {
            for (final Entity deletedEntity : lastDeleted)
              comboModel.removeItem(deletedEntity);
            if (comboModel.getSize() > 0)
              comboModel.setSelectedItem(comboModel.getElementAt(0));
            else
              comboModel.setSelectedItem(null);
          }
        }
      }
    }
  }

  protected void refreshDetailModelsAfterInsertOrUpdate() throws UserException {
    for (final EntityModel detailModel : detailModels) {
      for (final Property.EntityProperty property :
              EntityRepository.get().getEntityProperties(detailModel.getEntityID(), getEntityID())) {
        final EntityComboBoxModel entityComboBoxModel =
                (EntityComboBoxModel) detailModel.propertyComboBoxModels.get(property);
        if (entityComboBoxModel != null)
          entityComboBoxModel.refresh();
      }
    }
  }

  protected void refreshEntityComboBoxModels() throws UserException {
    log.trace(this + " refreshing EntityComboBoxModels");
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values()) {
      if (comboBoxModel instanceof EntityComboBoxModel)
        ((EntityComboBoxModel) comboBoxModel).refresh();
    }
  }

  protected void refreshDetailModels() throws UserException {
    log.trace(this + " refreshing detail models");
    for (final EntityModel detailModel : detailModels)
      detailModel.refresh();
  }

  protected final void updateDetailModelsByActiveEntity() throws UserException {
    for (final EntityModel detailModel : linkedDetailModels) {
      if (getTableModel() != null)
        detailModel.masterSelectionChanged(getTableModel().stSelectionEmpty.isActive()
                ? null : getTableModel().getSelectedEntities(), getEntityID());
      else
        detailModel.masterSelectionChanged(isActiveEntityNull() ? null : getActiveEntityCopy(), getEntityID());
    }
  }

  protected Object doSetValue(final Property property, final Object value, final boolean validate) {
    activeEntity.setValue(property.propertyID, value, validate);

    return value;
  }

  /**
   * Sets the ComboBoxModel to be associated with the given property
   * @param property the property
   * @param model the ComboBoxModel
   * @throws RuntimeException in case the ComboBoxModel has already been set for this property
   */
  private void setComboBoxModel(final Property property, final ComboBoxModel model) {
    if (propertyComboBoxModels.containsKey(property))
      throw new RuntimeException("ComboBoxModel already associated with property: " + property);

    propertyComboBoxModels.put(property, model);
  }

  private void setActiveEntityWriteLock(final boolean status) throws UserException {
    if (strictEditLockEnabled == status)
      return;
    try {
      if (status)
        getEntityDb().selectForUpdate(activeEntity.getPrimaryKey());
      else
        getEntityDb().endTransaction(true);

      System.out.println("######################### " + (status ? "locked" : "unlocked") + ": " + activeEntity.getPrimaryKey());
      strictEditLockEnabled = status;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void notifyPropertyChanged(final Property property, final Object newValue, final Object oldValue,
                                     final boolean isModelChange) {
    if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT)) {
      final String msg = getPropertyChangeDebugString(property, oldValue, newValue, isModelChange);
      System.out.println(msg);
      log.trace(msg);
    }
    final PropertyChangeEvent propEvent = new PropertyChangeEvent(property, newValue, oldValue,
            isModelChange, false);
    if (isModelChange)
      getPropertyModelChangeEvent(property).fire(propEvent);
    else
      getPropertyUIChangeEvent(property).fire(propEvent);
  }

  private String getPropertyChangeDebugString(final Property property, final Object oldValue,
                                              final Object newValue, final boolean isModelChange) {
    final String simpleClassName = getClass().getSimpleName();
    final StringBuffer ret = new StringBuffer().append(simpleClassName.length() > 0 ? (simpleClassName + " ") : "");
    ret.append(isModelChange ? "MODEL SET" : "UI SET").append(Util.equal(oldValue, newValue) ? " == " : " <> ");
    ret.append(getEntityID()).append(" -> ").append(property).append("; ");
    if (oldValue != null)
      ret.append(oldValue.getClass().getSimpleName()).append(" ");
    ret.append(EntityUtil.getValueString(property, oldValue));
    ret.append(" : ");
    if (newValue != null)
      ret.append(newValue.getClass().getSimpleName()).append(" ");
    ret.append(EntityUtil.getValueString(property, newValue));

    return ret.toString();
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final Class<Entity> entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, null);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final Class<Entity> entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider, final Event refreshEvent) {
    return createPropertyComboBoxModel(entityID.getName(), property, dbProvider, refreshEvent);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, null);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider, final Event refreshEvent) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, refreshEvent, null);
  }

  /**
   * @param entityID the class of the entity for which to create a PropertyComboBoxModel
   * @param property the property for which to create the PropertyComboBoxModel
   * @param dbProvider the dbProvider instance used for retrieving the values for this PropertyComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires
   * @param nullValue the null value at the top of the list
   * @return a PropertyComboBoxModel containing the distinct values found for the given property
   */
  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider, final Event refreshEvent,
                                                                  final Object nullValue) {
    try {
      if (property == null)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel without a property");
      if (property instanceof Property.EntityProperty)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel for a reference property "
                + property.propertyID + ",\nuse an EntityComboBoxModel instead!");
      final PropertyComboBoxModel comboBoxModel =
              new PropertyComboBoxModel(dbProvider, entityID, property, nullValue);

      comboBoxModel.refresh();

      if (refreshEvent != null) {
        refreshEvent.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
              comboBoxModel.refresh();
            }
            catch (UserException ex) {
              throw ex.getRuntimeException();
            }
          }
        });
      }

      return comboBoxModel;
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }
}