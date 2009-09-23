/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.log4j.Logger;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A model class with basic functionality for creating, editing and deleting objects from a database
 */
public class EntityModel implements Refreshable {

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
   * Fired when the model is about to be refreshed
   */
  public final Event evtRefreshStarted = new Event();

  /**
   * Fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public final Event evtRefreshDone = new Event();

  /**
   * Fired when the model has been cleared
   */
  public final Event evtModelCleared = new Event();

  /**
   * Fired when detail models are linked or unlinked
   */
  public final Event evtLinkedDetailModelsChanged = new Event();

  /**
   * If this state is active a refresh of this model triggers a refresh in all detail models
   */
  private final State stCascadeRefresh = new State();

  /**
   * This state determines whether this model allowes records to be inserted
   * @see #setInsertAllowed(boolean)
   * @see #isInsertAllowed()
   */
  private final State stAllowInsert = new State(true);

  /**
   * This state determines whether this model allowes records to be udpated
   * @see #setUpdateAllowed(boolean)
   * @see #isUpdateAllowed()
   */
  private final State stAllowUpdate = new State(true);

  /**
   * This state determines whether this model allowes records to be deleted
   * @see #setDeleteAllowed(boolean)
   * @see #isDeleteAllowed()
   */
  private final State stAllowDelete = new State(true);

  /**
   * The ID of the Entity this EntityModel represents
   */
  private final String entityID;

  /**
   * The EntityDb connection provider
   */
  private final EntityDbProvider dbProvider;

  /**
   * The EntityEditModel instance
   */
  private final EntityEditModel editModel;

  /**
   * The table model
   */
  private final EntityTableModel tableModel;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final List<EntityModel> detailModels = new ArrayList<EntityModel>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final List<EntityModel> linkedDetailModels = new ArrayList<EntityModel>();

  /**
   * Indicates whether selection in a master model triggers the filtering of this model
   */
  private boolean selectionFiltersDetail = true;

  /**
   * The master model, if any, so that detail models can refer to their masters
   */
  private EntityModel masterModel;

  /**
   * True while the model is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * Instantiates a new EntityModel
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a EntityDbProvider
   * @throws UserException in case of an exception
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider) throws UserException {
    this(entityID, dbProvider, true);
  }

  /**
   * Instantiates a new EntityModel
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a EntityDbProvider
   * @param includeTableModel true if this EntityModel should include a table model
   * @throws UserException in case of an exception
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider,
                     final boolean includeTableModel) throws UserException {
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.editModel = intializeEditModel();
    this.tableModel = includeTableModel ? initializeTableModel() : null;
    addDetailModels();
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
   * @return the database connection provider
   */
  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * Returns the EntityDb connection, the instance returned by this
   * method should not be viewed as long lived since it does not survive
   * network connection outages for example
   * @return the database connection
   * @throws UserException in case of an exception
   */
  public EntityDb getEntityDb() throws UserException {
    return getDbProvider().getEntityDb();
  }

  /**
   * @return a String represention of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * @return true if this model is read only,
   * by default this returns the isReadOnly value of the underlying entity
   */
  public boolean isReadOnly() {
    return EntityRepository.isReadOnly(getEntityID());
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
  public boolean isSelectionFiltersDetail() {
    return selectionFiltersDetail;
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
    selectionFiltersDetail = value;
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
   * @return the EntityEditor instance used by this EntityModel
   */
  public EntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * @return the EntityTableModel, null if none is specified
   */
  public EntityTableModel getTableModel() {
    return tableModel;
  }

  /**
   * @return true if this EntityModel contains a EntityTableModel
   */
  public boolean containsTableModel() {
    return getTableModel() != null;
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
    getEditModel().setEntity(null);
    evtModelCleared.fire();
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

    log.debug(toString() + " - insert "+ Util.getListContentsAsString(entities, false));

    evtBeforeInsert.fire();
    validateData(entities, INSERT);

    final List<EntityKey> primaryKeys = EntityKey.copyEntityKeys(doInsert(entities));
    if (containsTableModel()) {
      getTableModel().getSelectionModel().clearSelection();
      getTableModel().addEntitiesByPrimaryKeys(primaryKeys, true);
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
    getEditModel().requestWriteLock(EntityUtil.getPrimaryKeys(entities));

    log.debug(toString() + " - update " + Util.getListContentsAsString(entities, false));

    final List<Entity> modifiedEntities = EntityUtil.getModifiedEntities(entities);
    if (modifiedEntities.size() == 0)
      return;

    evtBeforeUpdate.fire();
    validateData(modifiedEntities, UPDATE);

    final List<Entity> updatedEntities = doUpdate(modifiedEntities);
    if (containsTableModel()) {
      if (Entity.isPrimaryKeyModified(modifiedEntities)) {
        getTableModel().refresh();//best we can do under the circumstances
      }
      else {//replace and select the updated entities
        final List<Entity> updated = new ArrayList<Entity>();
        for (final Entity entity : updatedEntities)
          if (entity.is(getEntityID()))
            updated.add(entity);
        getTableModel().replaceEntities(updated);
        getTableModel().setSelectedEntities(updated);
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
    log.debug(toString() + " - delete " + Util.getListContentsAsString(entities, false));

    evtBeforeDelete.fire();
    if (containsTableModel())
      getTableModel().getSelectionModel().clearSelection();

    doDelete(entities);
    if (containsTableModel())
      getTableModel().removeEntities(entities);

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
    return Arrays.asList(getEditModel().getEntityCopy());
  }

  /**
   * Default behaviour is returning a List containing a copy of the active entity
   * This method should return an empty List instead of null.
   * @return the entities to use when update is triggered
   * @see #update()
   */
  public List<Entity> getEntitiesForUpdate() {
    return Arrays.asList(getEditModel().getEntityCopy());
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
    return containsTableModel() ? getTableModel().getSelectedEntities() :
            getEditModel().isEntityNull() ? new ArrayList<Entity>() :  Arrays.asList(getEditModel().getEntityCopy());
  }

  /**
   * Takes a path to a report which uses a JDBC datasource and returns an initialized JasperPrint object
   * @param reportPath the path to the report file
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws Exception in case of exception
   */
  public JasperPrint fillJdbcReport(final String reportPath, final Map reportParameters) throws Exception {
    return EntityReportUtil.fillJdbcReport(getDbProvider().getEntityDb(), reportPath, reportParameters);
  }

  /**
   * Takes a path to a report and returns an initialized JasperPrint object using the
   * datasource returned by <code>getTableModel().getJRDataSource()</code> method
   * @param reportPath the path to the report file
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws Exception in case of exception
   * @see org.jminor.framework.client.model.EntityTableModel#getJRDataSource()
   */
  public JasperPrint fillReport(final String reportPath, final Map reportParameters) throws Exception {
    return fillReport(reportPath, reportParameters, getTableModel().getJRDataSource());
  }

  /**
   * Takes a path to a report which uses a JRDataSource and returns an initialized JasperPrint object
   * @param reportPath the path to the report file, this path can be http or file based
   * @param reportParameters the report parameters
   * @param dataSource the JRDataSource used to provide the report data
   * @return an initialized JasperPrint object
   * @throws net.sf.jasperreports.engine.JRException in case of a report exception
   * @throws Exception in case of exception
   */
  public JasperPrint fillReport(final String reportPath, final Map reportParameters,
                                final JRDataSource dataSource) throws Exception {
    return EntityReportUtil.fillReport(reportPath, reportParameters, dataSource);
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

      getEditModel().refreshComboBoxModels();
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
   * Updates this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterValues the master entities
   * @param masterEntityID the ID of the master entity
   * @throws UserException in case of a problem
   */
  public void masterSelectionChanged(final List<Entity> masterValues, final String masterEntityID) throws UserException {
    if (isSelectionFiltersDetail() && containsTableModel())
      getTableModel().filterByReference(masterValues, masterEntityID);

    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(getEntityID(), masterEntityID))
      getEditModel().setValue(foreignKeyProperty, masterValues != null && masterValues.size() > 0 ? masterValues.get(0) : null);//todo
  }

  /**
   * @return a List of EntityModels serving as detail models
   * @throws UserException in case of an exception
   */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return new ArrayList<EntityModel>(0);
  }

  /**
   * Override this method to initialize any associated models before bindEvents() is called.
   * An associated model could for example be an EntityModel that is used by this model but is not a detail model.
   * @throws UserException in case of an exception
   */
  protected void initializeAssociatedModels() throws UserException {}

  /**
   * Validates the given Entity objects
   * For overriding
   * @param entities the entities to validate
   * @param action describes the action requiring validation, EntityModel.INSERT or EntityModel.UPDATE
   * @throws UserException in case the validation fails
   * @see #INSERT
   * @see #UPDATE
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void validateData(final List<Entity> entities, final int action) throws UserException {}

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
      getEntityDb().delete(EntityUtil.getPrimaryKeys(entities));
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
   * @return the EntityEditMOdel used by this EntityModel
   */
  protected EntityEditModel intializeEditModel() {
    return new EntityEditModel(getEntityID(), getDbProvider(), evtEntitiesChanged);
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
          if (!getEditModel().isEntityNull())
            updateDetailModelsByActiveEntity();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    if (!containsTableModel()) {
      getEditModel().getEntityChangedEvent().addListener(new ActionListener() {
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
    if (!containsTableModel())
      return;

    getTableModel().evtSelectionChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          updateDetailModelsByActiveEntity();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });

    getTableModel().evtSelectedIndexChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        getEditModel().setEntity(getTableModel().getSelectionModel().isSelectionEmpty() ? null : getTableModel().getSelectedEntity());
      }
    });

    getTableModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent event) {
        //if the selected record is being updated via the table model refresh the one in the model
        if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == getTableModel().getSelectedIndex()) {
          getEditModel().setEntity(null);
          getEditModel().setEntity(getTableModel().getSelectedEntity());
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
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityComboBoxModel comboModel = detailModel.getEditModel().getEntityComboBoxModel(foreignKeyProperty);
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
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityComboBoxModel entityComboBoxModel = detailModel.getEditModel().getEntityComboBoxModel(foreignKeyProperty);
          if (entityComboBoxModel != null)
            entityComboBoxModel.refresh();
          detailModel.getEditModel().setValue(foreignKeyProperty, insertedEntity);
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
      for (final Property.ForeignKeyProperty foreignKeyProperty :
              EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
        final EntityComboBoxModel entityComboBoxModel = detailModel.getEditModel().getEntityComboBoxModel(foreignKeyProperty);
        if (entityComboBoxModel != null)
          entityComboBoxModel.refresh();
      }
    }
  }

  protected void refreshDetailModels() throws UserException {
    log.trace(this + " refreshing detail models");
    for (final EntityModel detailModel : detailModels)
      detailModel.refresh();
  }

  protected void updateDetailModelsByActiveEntity() throws UserException {
    final List<Entity> activeEntities = containsTableModel() ?
            (getTableModel().stSelectionEmpty.isActive() ? null : getTableModel().getSelectedEntities()) :
            (getEditModel().isEntityNull() ? null : Arrays.asList(getEditModel().getEntityCopy()));
    for (final EntityModel detailModel : linkedDetailModels)
      detailModel.masterSelectionChanged(activeEntities, getEntityID());
  }

  private void addDetailModels() throws UserException {
    final boolean filterQueryByMaster = (Boolean) Configuration.getValue(Configuration.FILTER_QUERY_BY_MASTER);
    for (final EntityModel detailModel : initializeDetailModels()) {
      detailModels.add(detailModel);
      detailModel.setMasterModel(this);
      if (detailModel.containsTableModel())
        detailModel.getTableModel().setQueryFilteredByMaster(filterQueryByMaster);
    }
  }

  private void setMasterModel(final EntityModel masterModel) {
    this.masterModel = masterModel;
  }
}