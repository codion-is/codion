/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.IRefreshable;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.PropertyListener;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.combobox.PropertyComboBoxModel;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;

import org.apache.log4j.Logger;

import javax.swing.ComboBoxModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model class with basic functionality for creating, editing and deleting objects from a database
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
  public final Event evtBeforeInsert = new Event();

  /**
   * Fired when an Entity has been inserted
   */
  public final Event evtAfterInsert = new Event();

  /**
   * Fired before an update is performed
   */
  public final Event evtBeforeUpdate = new Event();

  /**
   * Fired when an Entity has been updated
   */
  public final Event evtAfterUpdate = new Event();

  /**
   * Fired before a delete is performed
   */
  public final Event evtBeforeDelete = new Event();

  /**
   * Fired when an Entity has been deleted
   */
  public final Event evtAfterDelete = new Event();

  /**
   * Fired when an entity is deleted, inserted or updated via this EntityModel
   */
  public final Event evtEntitiesChanged = new Event();

  /**
   * Fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public final Event evtRefreshDone = new Event();

  /**
   * Fired when the model is about to be refreshed
   */
  public final Event evtRefreshStarted = new Event();

  /**
   * Fired when the model has been cleared
   */
  public final Event evtModelCleared = new Event();

  /**
   * Fired when the active entity is about to be changed
   */
  public final Event evtActiveEntityChanging = new Event();

  /**
   * Fired when the active entity is changed
   */
  public final Event evtActiveEntityChanged = new Event();

  /**
   * Active when a non-null entity is active
   */
  public final State stEntityActive = new State("EntityModel.stEntityActive");

  /**
   * Fired when detail models are linked or unlinked
   */
  public final Event evtLinkedDetailModelsChanged = new Event();

  /**
   * If this state is active selection in this model triggers the filtering of all linked detail models
   */
  private final State stSelectionFiltersDetail = new State("EntityModel.stSelectionFiltersDetail", true);

  /**
   * If this state is active a refresh of this model triggers a refresh in all detail models
   */
  private final State stCascadeRefresh = new State("EntityModel.stCascadeRefresh", false);

  /**
   * This state determines whether this model allowes records to be inserted
   * @see #setInsertAllowed(boolean)
   * @see #isInsertAllowed()
   */
  private final State stAllowInsert = new State("EntityModel.stAllowInsert", true);

  /**
   * This state determines whether this model allowes records to be udpated
   * @see #setUpdateAllowed(boolean)
   * @see #isUpdateAllowed()
   */
  private final State stAllowUpdate = new State("EntityModel.stAllowUpdate", true);

  /**
   * This state determines whether this model allowes records to be deleted
   * @see #setDeleteAllowed(boolean)
   * @see #isDeleteAllowed()
   */
  private final State stAllowDelete = new State("EntityModel.stAllowDelete", true);

  /**
   * The table model
   */
  private final EntityTableModel tableModel;

  /**
   * The IEntityDb connection provider
   */
  private final IEntityDbProvider dbProvider;

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
   * @see IRefreshable
   */
  private final Map<Property, ComboBoxModel> propertyComboBoxModels;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final List<? extends EntityModel> detailModels;

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final List<EntityModel> linkedDetailModels = new ArrayList<EntityModel>();

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
   * The master model, if any, so that detail models can refer to their masters
   */
  private EntityModel masterModel;

  /**
   * Contains the locked entities while the strict editing lock is in effect (select for update)
   */
  private Set<Entity> lockedEntities = new HashSet<Entity>();

  /**
   * True while the model is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * Initiates a new EntityModel
   * @param caption a caption describing this EntityModel
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a IEntityDbProvider
   * @throws UserException in case of an exception
   */
  public EntityModel(final String caption, final String entityID, final IEntityDbProvider dbProvider) throws UserException {
    this(caption, entityID, dbProvider, true);
  }

  /**
   * Initiates a new EntityModel
   * @param caption a caption describing this EntityModel
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a IEntityDbProvider
   * @param includeTableModel true if this EntityModel should include a table model
   * @throws UserException in case of an exception
   */
  public EntityModel(final String caption, final String entityID, final IEntityDbProvider dbProvider,
                     final boolean includeTableModel) throws UserException {
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    this.caption = caption;
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.propertyComboBoxModels = initializeEntityComboBoxModels();
    this.tableModel = includeTableModel ? initializeTableModel() : null;
    this.activeEntity = new Entity(entityID);
    this.activeEntity.setAs(getDefaultEntity());
    this.detailModels = initializeDetailModels();
    this.activeEntity.setFirePropertyChangeEvents(true);
    final boolean filterQueryByMaster = (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.FILTER_QUERY_BY_MASTER);
    for (final EntityModel detailModel : this.detailModels) {
      detailModel.setMasterModel(this);
      if (detailModel.getTableModel() != null)
        detailModel.getTableModel().setFilterQueryByMaster(filterQueryByMaster);
    }
    initializeAssociatedModels();
    bindEvents();
    bindTableModelEvents();
  }

  /**
   * @return the ID of the entity this model represents
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
      releaseWriteLock();

    this.strictEditingEnabled = strictEditMode;
  }

  /**
   * @return a String represention of this EntityModel,
   * returns the model caption by default
   */
  @Override
  public String toString() {
    return getCaption();
  }

  /**
   * @return the database connection provider
   */
  public IEntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * Returns the IEntityDb connection, the instance returned by this
   * method should not be viewed as long lived since it does not survive
   * network connection outages for example
   * @return the database connection
   * @throws UserException in case of an exception
   */
  public IEntityDb getEntityDb() throws UserException {
    return getDbProvider().getEntityDb();
  }

  /**
   * @return the caption
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return true if this model is read only,
   * by default this returns the isReadOnly value of the underlying entity
   */
  public boolean isReadOnly() {
    return EntityRepository.get().isReadOnly(entityID);
  }

  /**
   * @return true if a refresh on this model should trigger a refresh in its detail models
   */
  public boolean getCascadeRefresh() {
    return stCascadeRefresh.isActive();
  }

  /**
   * @param value true if a refresh in this model should trigger a refresh in its detail models
   */
  public void setCascadeRefresh(final boolean value) {
    for (final EntityModel detailModel : detailModels)
      detailModel.setCascadeRefresh(value);

    stCascadeRefresh.setActive(value);
  }

  /**
   * @return true if the selecting a record in this model should filter the detail models
   */
  public boolean getSelectionFiltersDetail() {
    return stSelectionFiltersDetail.isActive();
  }

  /**
   * @param value true if selecting a record in this model should filter the detail models
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
  public boolean isMultipleUpdateAllowed() {
    return true;
  }

  /**
   * @return true if this model should allow records to be inserted
   */
  public boolean isInsertAllowed() {
    return stAllowInsert.isActive();
  }

  /**
   * @param value true if this model should allow inserts
   */
  public void setInsertAllowed(final boolean value) {
    stAllowInsert.setActive(value);
  }

  /**
   * @return the state used to determine if inserting should be enabled
   * @see #isInsertAllowed()
   * @see #setInsertAllowed(boolean)
   */
  public State getInsertAllowedState() {
    return stAllowInsert;
  }

  /**
   * @return true if this model should allow records to be updated
   */
  public boolean isUpdateAllowed() {
    return stAllowUpdate.isActive();
  }

  /**
   * @param value true if this model should allow records to be updated
   */
  public void setUpdateAllowed(final boolean value) {
    stAllowUpdate.setActive(value);
  }

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isUpdateAllowed()
   * @see #setUpdateAllowed(boolean)
   */
  public State getUpdateAllowedState() {
    return stAllowUpdate;
  }

  /**
   * @return true if this model should allow records to be deleted
   */
  public boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  /**
   * @param value true if this model should allow records to be deleted
   */
  public void setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
  }

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  public State getDeleteAllowedState() {
    return stAllowDelete;
  }

  /**
   * @return the master model, if any
   */
  public EntityModel getMasterModel() {
    return masterModel;
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
   * Sets the currently linked detail models. Linked models are updated and filtered according
   * to the entity/entities selected in this (the master) model
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

    throw new RuntimeException("No detail model of type " + entityModelClass + " found in model: " + this);
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
   * @return a deep copy of the active entity
   * @see org.jminor.framework.model.Entity#getCopy()
   */
  public Entity getActiveEntityCopy() {
    return activeEntity.getCopy();
  }

  /**
   * @return the state which indicates the modified state of the active entity
   * @see org.jminor.framework.model.Entity#getModifiedState()
   */
  public State getActiveEntityModifiedState() {
    return activeEntity.getModifiedState();
  }

  /**
   * @return true if the active entity has been modified
   * @see org.jminor.framework.model.Entity#isModified()
   */
  public boolean isActiveEntityModified() {
    return getActiveEntityModifiedState().isActive();
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

    final Event ret = new Event();
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

    final Event ret = new Event();
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

    final Event ret = new Event();
    activeEntity.getPropertyChangeEvent().addListener(new PropertyListener() {
      @Override
      protected void propertyChanged(final PropertyChangeEvent event) {
        if (event.getProperty().equals(property))
          ret.fire(event);
      }
    });
    changeEventMap.put(property, ret);

    return ret;
  }

  /**
   * Sets the active entity, that is, the entity to be edited
   * @param entity the entity to set as active, if null then the default entity value is set as active
   * @see #evtActiveEntityChanging
   * @see #evtActiveEntityChanged
   */
  public final void setActive(final Entity entity) {
    if (entity != null && activeEntity.propertyValuesEqual(entity))
      return;
    if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT) && entity != null)
      Entity.printPropertyValues(entity);

    evtActiveEntityChanging.fire();

    activeEntity.setAs(entity == null ? getDefaultEntity() : entity);

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

  /**
   * @param property the property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public final Object getValue(final Property property) {
    return getValue(property.propertyID);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public final Object getValue(final String propertyID) {
    return activeEntity.getValue(propertyID);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the value
   * @return the value associated with the property identified by <code>propertyID</code>
   */
  public final Entity getEntityValue(final String propertyID) {
    return activeEntity.getEntityValue(propertyID);
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value associated with <code>property</code>
   */
  public final Entity getEntityValue(final Property.EntityProperty property) {
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
   * @see #evtAfterInsert
   */
  public final void insert(final List<Entity> entities) throws UserException, DbException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, inserting is not allowed!");
    if (!isInsertAllowed())
      throw new UserException("This model does not allow inserting!");

    log.debug(caption + " - insert "+ Util.getListContentsAsString(entities, false));

    evtBeforeInsert.fire();
    validateData(entities, INSERT);

    final List<EntityKey> primaryKeys = EntityKey.copyEntityKeys(doInsert(entities));
    if (tableModel != null) {
      tableModel.getSelectionModel().clearSelection();
      tableModel.addEntitiesByPrimaryKeys(primaryKeys, true);
    }

    evtAfterInsert.fire(new InsertEvent(this, primaryKeys));
    refreshDetailModelsAfterInsert(primaryKeys);
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
   * Updates the given Entities and selects the updated entities in the table model if one is available
   * If the entities are unmodified this method returns silently.
   * @param entities the Entities to update
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #evtBeforeUpdate
   * @see #evtAfterUpdate
   */
  public final void update(final List<Entity> entities) throws UserException, DbException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, updating is not allowed!");
    if (!isMultipleUpdateAllowed() && entities.size() > 1)
      throw new UserException("Update of multiple entities is not allowed!");
    if (!isUpdateAllowed())
      throw new UserException("This model does not allow updating!");
    if (strictEditingEnabled)
      requestWriteLock(entities);

    log.debug(caption + " - update " + Util.getListContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.size() == 0)
      return;

    evtBeforeUpdate.fire();
    validateData(modifiedEntities, UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    if (tableModel != null) {
      if (EntityUtil.isPrimaryKeyModified(modifiedEntities)) {
        tableModel.refresh();//best we can do under the circumstances
      }
      else {//replace and select the updated entities
        final List<Entity> updated = new ArrayList<Entity>();
        for (final Entity entity : updatedEntities)
          if (entity.is(getEntityID()))
            updated.add(entity);
        tableModel.replaceEntities(updated);
        tableModel.setSelectedEntities(updated);
      }
    }

    evtAfterUpdate.fire(new UpdateEvent(this, updatedEntities));
    refreshDetailModelsAfterUpdate(updatedEntities);
  }

  /**
   * Deletes the entities returned by getEntitiesForDelete()
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   * @throws UserCancelException in case the user cancels the operation
   * @see #getEntitiesForDelete()
   * @see #evtBeforeDelete
   * @see #evtAfterDelete
   */
  public final void delete() throws DbException, UserException, UserCancelException {
    if (isReadOnly())
      throw new UserException("This is a read-only model, deleting is not allowed!");
    if (!isDeleteAllowed())
      throw new UserException("This model does not allow deleting!");

    final List<Entity> entities = getEntitiesForDelete();
    log.debug(caption + " - delete " + Util.getListContentsAsString(entities, false));

    evtBeforeDelete.fire();
    if (tableModel != null)
      tableModel.getSelectionModel().clearSelection();

    doDelete(entities);
    if (tableModel != null)
      tableModel.removeEntities(entities);

    evtAfterDelete.fire(new DeleteEvent(this, entities));
    refreshDetailModelsAfterDelete(entities);
  }

  /**
   * Default behaviour is returning a List containing a copy of the active entity
   * This method should return an empty List instead of null.
   * @return the entities to use when insert is triggered
   * @see #insert()
   */
  public List<Entity> getEntitiesForInsert() {
    return Arrays.asList(getActiveEntityCopy());
  }

  /**
   * Default behaviour is returning a List containing a copy of the active entity
   * This method should return an empty List instead of null.
   * @return the entities to use when update is triggered
   * @see #update()
   */
  public List<Entity> getEntitiesForUpdate() {
    return Arrays.asList(getActiveEntityCopy());
  }

  /**
   * Returns the entities to use when delete is triggered.
   * Default behaviour is returning the selected entities
   * from the table model or if no table model is available
   * a List containing a copy of the active entity.
   * This method should return an empty List instead of null.
   * @return the entities to use when delete is triggered
   * @see #delete()
   */
  public List<Entity> getEntitiesForDelete() {
    return getTableModel() != null ? getTableModel().getSelectedEntities() :
            isActiveEntityNull() ? new ArrayList<Entity>() :  Arrays.asList(getActiveEntityCopy());
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
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      log.trace(this + " done refreshing");
    }
  }

  /**
   * Refreshes the IRefreshable ComboBoxModels associated with this EntityModel
   * @throws UserException in case of an exception
   * @see IRefreshable
   */
  public void refreshComboBoxModels() throws UserException {
    for (final ComboBoxModel comboBoxModel : propertyComboBoxModels.values())
      if (comboBoxModel instanceof IRefreshable)
        ((IRefreshable) comboBoxModel).refresh();
  }

  /**
   * Updates this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterValues the master entities
   * @param masterEntityID the ID of the master entity
   * @throws UserException in case of a problem
   */
  public void masterSelectionChanged(final List<Entity> masterValues, final String masterEntityID) throws UserException {
    if (stSelectionFiltersDetail.isActive() && tableModel != null)
      tableModel.filterByReference(masterValues, masterEntityID);

    for (final Property.EntityProperty property : EntityRepository.get().getEntityProperties(getEntityID(), masterEntityID))
      setValue(property, masterValues != null && masterValues.size() > 0 ? masterValues.get(0) : null);
  }

  /**
   * @param property the property for which to get the ComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.evtEntitiesChanged is used
   * @param nullValue the value to use for representing the null item at the top of the list,
   * if this value is null then no such item is included
   * @return a ComboBoxModel representing <code>property</code>, if no combo box model
   * has been initialized for the given property, a new one is created and associated with
   * the property, to be returned the next time this method is called
   */
  public ComboBoxModel getPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                final Object nullValue) {
    PropertyComboBoxModel ret = (PropertyComboBoxModel) propertyComboBoxModels.get(property);
    if (ret == null)
      setComboBoxModel(property, ret =
            createPropertyComboBoxModel(property, refreshEvent == null ? evtEntitiesChanged : refreshEvent, nullValue));

    return ret;
  }

  /**
   * @param property the property for which to create the PropertyComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires
   * @param nullValue the value to appear at the top of the list, representing null
   * @return a PropertyComboBoxModel containing the distinct values found for the given property
   */
  public PropertyComboBoxModel createPropertyComboBoxModel(final Property property, final Event refreshEvent,
                                                           final Object nullValue) {
    try {
      if (property == null)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel without a property");
      if (property instanceof Property.EntityProperty)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel for a reference property "
                + property.propertyID + ",\nuse an EntityComboBoxModel instead!");
      final PropertyComboBoxModel comboBoxModel = new PropertyComboBoxModel(getEntityID(), getDbProvider(), property, nullValue);

      comboBoxModel.refresh();

      if (refreshEvent != null) {
        refreshEvent.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent event) {
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

  /**
   * @param propertyID the ID of the property for which to retrieve the <code>EntityComboBoxModel</code>
   * @return the EntityComboBoxModel for the property identified by <code>propertyID</code>,
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @see #initializeEntityComboBoxModels()
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
   * if no combo box model is associated with the property a new one is initialized, and associated
   * with the given property
   * @see #initializeEntityComboBoxModels()
   */
  public EntityComboBoxModel getEntityComboBoxModel(final Property.EntityProperty property) {
    EntityComboBoxModel ret = (EntityComboBoxModel) propertyComboBoxModels.get(property);
    if (ret == null)
      setComboBoxModel(property, ret = createEntityComboBoxModel(property));

    return ret;
  }

  /**
   * Creates a default EntityComboBoxModel for the given property, override to provide
   * specific EntityComboBoxModels (filtered for example) for properties.
   * This method is called when creating a EntitComboBoxModel for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted EntityComboBoxModel with "-" as the nullValueItem
   * @param property the property for which to create a EntityComboBoxModel
   * @return a EntityComboBoxModel for the given property
   */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.EntityProperty property) {
    return createEntityComboBoxModel(property, "-", true);
  }

  /**
   * Creates a default EntityComboBoxModel for the given property, override to provide
   * specific EntityComboBoxModels (filtered for example) for properties.
   * This method is called when creating a EntitComboBoxModel for entity properties, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records
   * @param property the property for which to create a EntityComboBoxModel
   * @param nullValueItem the item used to represent a null value
   * @param sortContents if true the contents are sorted
   * @return a EntityComboBoxModel for the given property
   */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.EntityProperty property,
                                                       final String nullValueItem, final boolean sortContents) {
    return new EntityComboBoxModel(property.referenceEntityID, getDbProvider(), false, nullValueItem, sortContents);
  }

  /**
   * Creates a EntityLookupModel for the given entityID
   * @param entityID the ID of the entity
   * @param additionalSearchCriteria an additional search criteria applied when performing the lookup
   * @param lookupProperties the properties involved in the lookup
   * @return a EntityLookupModel
   */
  public EntityLookupModel createEntityLookupModel(final String entityID, final ICriteria additionalSearchCriteria,
                                                   final List<Property> lookupProperties) {
    return new EntityLookupModel(entityID, getDbProvider(), additionalSearchCriteria, lookupProperties);
  }

  /**
   * @param classes the classes that should be returned in a List
   * @return the classes in a List
   */
  public static List<Class<? extends EntityModel>> asList(final Class<? extends EntityModel>... classes) {
    return new ArrayList<Class<? extends EntityModel>>(Arrays.asList(classes));
  }

  /**
   * @return a List of EntityModels serving as detail models
   * @throws UserException in case of an exception
   */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return new ArrayList<EntityModel>(0);
  }

  /**
   * Override this method to initialize any associated EntityModel before bindEvents() is called.
   * Associated models are EntityModels that are used by this model but are not detail models.
   * @throws UserException in case of an exception
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
   * Returns a Map, mapping the provided EntityComboBoxModels to their respective properties according to the entityID.
   * This implementation maps the EntityComboBoxModel to the Property.EntityProperty with the same entityID.
   * If the underlying Entity references the same Entity via more than one property, this method throws a RuntimeException.
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
      if (properties.size() > 1)
        throw new RuntimeException("Multiple possible properties found for EntityComboBoxModel: " + comboBoxModel);
      else if (properties.size() == 1)
        ret.put(properties.get(0), comboBoxModel);
      else
        throw new RuntimeException("Property not found for EntityComboBoxModel: " + comboBoxModel);
    }

    return ret;
  }

  /**
   * Validates the given Entity objects
   * For overriding
   * @param entities the entities to validate
   * @param action describes the action requiring validation, INSERT or UPDATE
   * @throws UserException in case of a user exception
   * @see #INSERT
   * @see #UPDATE
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
   * @return the default entity for this EntitModel, it is set as active when no item is selected
   */
  protected Entity getDefaultEntity() {
    final Entity ret = new Entity(getEntityID());
    for (final Property property : EntityRepository.get().getDatabaseProperties(getEntityID()))
      if (!property.hasParentProperty() && !(property instanceof Property.DenormalizedProperty))//these are set via their respective parent properties
        ret.setValue(property, getDefaultValue(property), true);

    return ret;
  }

  /**
   * Returns the default value for the given property, used when initializing a new
   * default entity for this model. This does not apply to denormalized properties
   * (Property.DenormalizedProperty) nor properties that are a part of reference properties
   * (Property.EntityProperty)
   * If the default value of a property should be the last value used <code>persistValueOnClear</code>
   * should be overridden so that it returns <code>true</code> for that property.
   * @param property the property
   * @return the default value for the property
   * @see #persistValueOnClear(org.jminor.framework.model.Property)
   */
  protected Object getDefaultValue(final Property property) {
    return persistValueOnClear(property) ? getValue(property) : property.getDefaultValue();
  }

  /**
   * Returns true if the last available value for this property should be used when initializing
   * a default entity for this EntityModel.
   * Override for selective reset of field values when the model is cleared.
   * For Property.EntityProperty values this method by default returns the value of the
   * property <code>FrameworkSettings.PERSIST_ENTITY_REFERENCE_VALUES</code>.
   * @param property the property
   * @return true if the given entity field value should be reset when the model is cleared
   * @see FrameworkSettings#PERSIST_ENTITY_REFERENCE_VALUES
   */
  protected boolean persistValueOnClear(final Property property) {
    return property instanceof Property.EntityProperty
            && (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PERSIST_ENTITY_REFERENCE_VALUES);
  }

  /**
   * Inserts the given entities from the database
   * @param entities the entities to insert
   * @return a list containing the primary keys of the inserted entities
   * @throws DbException in case of a database exception
   * @throws UserException in case of a exception
   * @throws UserCancelException in case the operation is cancelled
   */
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

  /**
   * Updates the given entities in the database
   * @param entities the entities to update
   * @return a list the udpated entities
   * @throws DbException in case of a database exception
   * @throws UserException in case of a exception
   * @throws UserCancelException in case the operation is cancelled
   */
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

  /**
   * Deletes the given entities from the database
   * @param entities the entities to delete
   * @throws DbException in case of a database exception
   * @throws UserException in case of a exception
   * @throws UserCancelException in case the operation is cancelled
   */
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

  /**
   * @return the EntityTableModel used by this EntityModel
   */
  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getEntityID(), getDbProvider());
  }

  /**
   * Override to add specific event bindings, remember to call super.bindEvents()
   */
  protected void bindEvents() {
    evtAfterDelete.addListener(evtEntitiesChanged);
    evtAfterInsert.addListener(evtEntitiesChanged);
    evtAfterUpdate.addListener(evtEntitiesChanged);
    evtLinkedDetailModelsChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          if (!isActiveEntityNull())
            updateDetailModelsByActiveEntity();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    activeEntity.getModifiedState().evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        try {
          if (strictEditingEnabled && !isActiveEntityNull()) {
            if (activeEntity.isModified())
              requestWriteLock(Arrays.asList(activeEntity));
            else
              releaseWriteLock();
          }
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    //always release the write lock if the entity being edited is de-selected
    evtActiveEntityChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          if (strictEditingEnabled)
            releaseWriteLock();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    if (tableModel == null) {
      evtActiveEntityChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          try {
            updateDetailModelsByActiveEntity();
          }
          catch (UserException ex) {
            throw ex.getRuntimeException();
          }
        }
      });
    }
  }

  /**
   * Override to add specific event bindings that depend on the table model,
   * remember to call super.bindTableModelEvents()
   */
  protected void bindTableModelEvents() {
    if (tableModel == null)
      return;

    tableModel.evtSelectionChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          updateDetailModelsByActiveEntity();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });

    tableModel.evtSelectedIndexChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        setActive(tableModel.getSelectionModel().isSelectionEmpty() ? null : tableModel.getSelectedEntity());
      }
    });

    tableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent event) {
        //if the selected record is being updated via the table model refresh the one in the model
        if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == tableModel.getSelectedIndex()) {
          setActive(null);
          setActive(tableModel.getSelectedEntity());
        }
      }
    });
  }

  /**
   * Removes the deleted entities from combobox models
   * @param deletedEntities the deleted entities
   */
  protected void refreshDetailModelsAfterDelete(final List<Entity> deletedEntities) {
    if (deletedEntities.size() > 0) {
      for (final EntityModel detailModel : detailModels) {
        for (final Property.EntityProperty property :
                EntityRepository.get().getEntityProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityComboBoxModel comboModel =
                  (EntityComboBoxModel) detailModel.propertyComboBoxModels.get(property);
          if (comboModel != null) {
            for (final Entity deletedEntity : deletedEntities)
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

  /**
   * Refreshes the EntityComboBoxModels based on the inserted entity type in the detail models
   * and sets the value of the master property to the entity with the primary key found
   * at index 0 in <code>primaryKeys</code>
   * @param primaryKeys the primary keys of the inserted entities
   * @throws UserException in case of an exception
   */
  protected void refreshDetailModelsAfterInsert(final List<EntityKey> primaryKeys) throws UserException {
    if (detailModels.size() == 0)
      return;

    try {
      final Entity insertedEntity = getEntityDb().selectSingle(primaryKeys.get(0));
      for (final EntityModel detailModel : detailModels) {
        for (final Property.EntityProperty property :
                EntityRepository.get().getEntityProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityComboBoxModel entityComboBoxModel =
                  (EntityComboBoxModel) detailModel.propertyComboBoxModels.get(property);
          if (entityComboBoxModel != null)
            entityComboBoxModel.refresh();
          detailModel.setValue(property, insertedEntity);
        }
      }
    }
    catch (UserException ue) {
      throw ue;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void refreshDetailModelsAfterUpdate(final List<Entity> entities) throws UserException {
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
        detailModel.masterSelectionChanged(isActiveEntityNull() ? null : Arrays.asList(getActiveEntityCopy()), getEntityID());
    }
  }

  protected Object doSetValue(final Property property, final Object value, final boolean validate) {
    activeEntity.setValue(property.propertyID, value, validate);

    return value;
  }

  private void setMasterModel(final EntityModel masterModel) {
    this.masterModel = masterModel;
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

  private void requestWriteLock(final List<Entity> entities) throws UserException {
    if (!strictEditingEnabled)
      throw new UserException("Strict editing mode must be enabled before requesting write lock");

    if (lockedEntities.containsAll(entities))
      return;

    try {
      entities.removeAll(lockedEntities);
      final List<EntityKey> keys = EntityUtil.getPrimaryKeys(entities);
      getEntityDb().selectForUpdate(keys);

      System.out.println("######################### " + "locked" + ": " + Util.getListContentsAsString(keys, false));
      lockedEntities.addAll(entities);
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void releaseWriteLock() throws UserException {
    if (!strictEditingEnabled)
      throw new UserException("Strict editing mode must be enabled before releasing write lock");

    if (lockedEntities.isEmpty())
      return;

    try {
      getEntityDb().endTransaction(false);

      System.out.println("######################### " + "unlocked");
      lockedEntities.clear();
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
    ret.append(Entity.getValueString(property, oldValue));
    ret.append(" : ");
    if (newValue != null)
      ret.append(newValue.getClass().getSimpleName()).append(" ");
    ret.append(Entity.getValueString(property, newValue));

    return ret.toString();
  }
}