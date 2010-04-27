/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapModel;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class responsible for, among other things, coordinating a EntityEditModel and an EntityTableModel.
 */
public class EntityModel extends ValueChangeMapModel<String, Object> implements EntityDbProvider {

  protected static final Logger log = Util.getLogger(EntityModel.class);

  private final Event evtEntitiesChanged = new Event();
  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();
  private final Event evtLinkedDetailModelsChanged = new Event();
  private final State stCascadeRefresh = new State();

  /**
   * The ID of the Entity this EntityModel represents
   */
  private final String entityID;

  /**
   * The EntityDb connection provider
   */
  private final EntityDbProvider dbProvider;

  /**
   * True if this EntityModel should contain a table model
   */
  private final boolean includeTableModel;

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
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, true);
  }

  /**
   * Instantiates a new EntityModel
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a EntityDbProvider
   * @param includeTableModel true if this EntityModel should include a table model
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider, final boolean includeTableModel) {
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.includeTableModel = includeTableModel;
    addDetailModels();
    initializeAssociatedModels();
    bindEventsInternal();
    bindEvents();
    bindTableModelEventsInternal();
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

  public void disconnect() {
    dbProvider.disconnect();
  }

  public String getDescription() {
    return dbProvider.getDescription();
  }

  /**
   * Returns the EntityDb connection, the instance returned by this
   * method should not be viewed as long lived since it does not survive
   * network connection outages for example
   * @return the database connection
   */
  public EntityDb getEntityDb() {
    return dbProvider.getEntityDb();
  }

  /**
   * @return a String representation of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * @return true if a refresh on this model should trigger a refresh in its detail models
   */
  public boolean isCascadeRefresh() {
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
   * @see EntityTableModel#filterByReference(String, java.util.List)
   */
  public void setSelectionFiltersDetail(final boolean value) {
    for (final EntityModel detailModel : detailModels)
      detailModel.setSelectionFiltersDetail(value);

    getTableModel().getSelectionModel().clearSelection();
    selectionFiltersDetail = value;
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
  @Override
  public EntityEditModel getEditModel() {
    return (EntityEditModel) super.getEditModel();
  }

  /**
   * @return the EntityTableModel, null if none is specified
   */
  @Override
  public EntityTableModel getTableModel() {
    return (EntityTableModel) super.getTableModel();
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
    return new ArrayList<EntityModel>(detailModels);
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
    return new ArrayList<EntityModel>(linkedDetailModels);
  }

  /**
   * Returns the detail model of the given type
   * @param entityModelClass the type of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  public EntityModel getDetailModel(final Class entityModelClass) {
    for (final EntityModel detailModel : detailModels)
      if (detailModel.getClass().equals(entityModelClass))
        return detailModel;

    throw new RuntimeException("No detail model of type " + entityModelClass + " found in model: " + this);
  }

  /**
   * Returns the detail model for the given entityID, this simply returns the first
   * detail model found, so if multiple detail models are based on the same entity, this method
   * is not reliable
   * @param entityID the ID of the Entity for which to find the detail model
   * @return the detail model representing the given entityID , null if none is found
   * @see #getDetailModel(Class)
   */
  public EntityModel getDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels)
      if (detailModel.getEntityID().equals(entityID))
        return detailModel;

    throw new RuntimeException("No detail model based on entityID " + entityID + " found in model: " + this);
  }



  /**
   * Takes a path to a report which uses a JDBC datasource and returns an initialized JasperPrint object
   * @param reportPath the path to the report file
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws JRException in case of a report exception
   */
  public JasperPrint fillJdbcReport(final String reportPath, final Map reportParameters) throws JRException {
    return EntityReportUtil.fillJdbcReport(getEntityDb(), reportPath, reportParameters);
  }

  /**
   * Takes a path to a report and returns an initialized JasperPrint object using the
   * datasource returned by <code>getTableModel().getJRDataSource()</code> method
   * @param reportPath the path to the report file
   * @param reportParameters the report parameters
   * @return an initialized JasperPrint object
   * @throws JRException in case of a report exception
   * @see EntityTableModel#getJRDataSource()
   */
  public JasperPrint fillReport(final String reportPath, final Map reportParameters) throws JRException {
    return fillReport(reportPath, reportParameters, getTableModel().getJRDataSource());
  }

  /**
   * Takes a path to a report which uses a JRDataSource and returns an initialized JasperPrint object
   * @param reportPath the path to the report file, this path can be http or file based
   * @param reportParameters the report parameters
   * @param dataSource the JRDataSource used to provide the report data
   * @return an initialized JasperPrint object
   * @throws JRException in case of a report exception
   */
  public JasperPrint fillReport(final String reportPath, final Map reportParameters,
                                final JRDataSource dataSource) throws JRException {
    return EntityReportUtil.fillReport(reportPath, reportParameters, dataSource);
  }

  /**
   * Refreshes this EntityModel
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   * @see #isCascadeRefresh
   */
  @Override
  public void refresh() {
    if (isRefreshing)
      return;

    try {
      log.trace(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      super.refresh();
      if (isCascadeRefresh())
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
   * @param masterEntityID the ID of the master entity
   * @param selectedMasterEntities the master entities
   */
  public void masterSelectionChanged(final String masterEntityID, final List<Entity> selectedMasterEntities) {
    if (isSelectionFiltersDetail() && containsTableModel())
      getTableModel().filterByReference(masterEntityID, selectedMasterEntities);

    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(getEntityID(), masterEntityID))
      getEditModel().setValue(foreignKeyProperty.getPropertyID(), selectedMasterEntities != null && selectedMasterEntities.size() > 0 ? selectedMasterEntities.get(0) : null);
  }

  /**
   * @return an Event fired when an entity is deleted, inserted or updated via this EntityModel
   */
  public Event eventEntitiesChanged() {
    return evtEntitiesChanged;
  }

  /**
   * @return an Event fired when detail models are linked or unlinked
   */
  public Event eventLinkedDetailModelsChanged() {
    return evtLinkedDetailModelsChanged;
  }

  /**
   * @return an Event fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public Event eventRefreshDone() {
    return evtRefreshDone;
  }

  /**
   * @return an Event fired when the model is about to be refreshed
   */
  public Event eventRefreshStarted() {
    return evtRefreshStarted;
  }

  /**
   * @return a List of EntityModels serving as detail models
   */
  protected List<? extends EntityModel> initializeDetailModels() {
    return new ArrayList<EntityModel>(0);
  }

  /**
   * Override this method to initialize any associated models before bindEvents() is called.
   * An associated model could for example be an EntityModel that is used by this model but is not a detail model.
   */
  protected void initializeAssociatedModels() {}

  /**
   * @return the EntityTableModel used by this EntityModel
   */
  @Override
  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getEntityID(), this);
  }

  /**
   * @return the EntityEditModel used by this EntityModel
   */
  @Override
  protected EntityEditModel initializeEditModel() {
    return includeTableModel ? new EntityEditModel(getEntityID(), this) : null;
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Override to add specific event bindings that depend on the table model
   */
  protected void bindTableModelEvents() {}

  /**
   * Removes the deleted entities from combobox models
   * @param deletedEntities the deleted entities
   */
  protected void refreshDetailModelsAfterDelete(final List<Entity> deletedEntities) {
    if (deletedEntities.size() > 0) {
      for (final EntityModel detailModel : detailModels) {
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityEditModel detailEditModel = detailModel.getEditModel();
          if (detailEditModel.containsComboBoxModel(foreignKeyProperty)) {
            final EntityComboBoxModel comboModel = detailEditModel.getEntityComboBoxModel(foreignKeyProperty);
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
   * at index 0 in <code>insertedPrimaryKeys</code>
   * @param insertedPrimaryKeys the primary keys of the inserted entities
   */
  protected void refreshDetailModelsAfterInsert(final List<Entity.Key> insertedPrimaryKeys) {
    if (detailModels.size() == 0)
      return;

    try {
      final Entity insertedEntity = getEntityDb().selectSingle(insertedPrimaryKeys.get(0));
      for (final EntityModel detailModel : detailModels) {
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
          final EntityEditModel detailEditModel = detailModel.getEditModel();
          if (detailEditModel.containsComboBoxModel(foreignKeyProperty))
            detailEditModel.getEntityComboBoxModel(foreignKeyProperty).refresh();
          detailEditModel.setValue(foreignKeyProperty.getPropertyID(), insertedEntity);
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void refreshDetailModelsAfterUpdate(final List<Entity> entities) {
    for (final EntityModel detailModel : detailModels) {
      for (final Property.ForeignKeyProperty foreignKeyProperty :
              EntityRepository.getForeignKeyProperties(detailModel.getEntityID(), getEntityID())) {
        final EntityEditModel detailEditModel = detailModel.getEditModel();
        if (detailEditModel.containsComboBoxModel(foreignKeyProperty))
          detailEditModel.getEntityComboBoxModel(foreignKeyProperty).refresh();
      }
    }
  }

  protected void refreshDetailModels() {
    log.trace(this + " refreshing detail models");
    for (final EntityModel detailModel : detailModels)
      detailModel.refresh();
  }

  protected void updateDetailModelsByActiveEntity() {
    final List<Entity> activeEntities = containsTableModel() ?
            (getTableModel().stateSelectionEmpty().isActive() ? null : getTableModel().getSelectedItems()) :
            (getEditModel().isEntityNull() ? null : Arrays.asList(getEditModel().getEntityCopy()));
    for (final EntityModel detailModel : linkedDetailModels)
      detailModel.masterSelectionChanged(getEntityID(), activeEntities);
  }

  private void addDetailModels() {
    for (final EntityModel detailModel : initializeDetailModels()) {
      detailModels.add(detailModel);
      detailModel.setMasterModel(this);
      if (detailModel.containsTableModel())
        detailModel.getTableModel().setQueryFilteredByMaster(true);
    }
  }

  private void setMasterModel(final EntityModel masterModel) {
    this.masterModel = masterModel;
  }

  private void bindEventsInternal() {
    getEditModel().eventAfterInsert().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          final List<Entity.Key> primaryKeys = ((InsertEvent) e).getInsertedKeys();
          if (containsTableModel()) {
            getTableModel().getSelectionModel().clearSelection();
            getTableModel().addEntitiesByPrimaryKeys(primaryKeys, true);
          }

          refreshDetailModelsAfterInsert(primaryKeys);
        }
        catch (DbException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    getEditModel().eventAfterUpdate().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> updatedEntities = ((UpdateEvent) e).getUpdatedEntities();
        if (containsTableModel()) {
          if (((UpdateEvent) e).isPrimaryKeyModified()) {
            getTableModel().refresh();//best we can do under the circumstances
          }
          else {//replace and select the updated entities
            final List<Entity> updated = new ArrayList<Entity>();
            for (final Entity entity : updatedEntities)
              if (entity.is(getEntityID()))
                updated.add(entity);
            getTableModel().replaceEntities(updated);
            getTableModel().setSelectedItems(updated);
          }
        }

        refreshDetailModelsAfterUpdate(updatedEntities);
      }
    });
    getEditModel().eventBeforeDelete().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (containsTableModel())
          getTableModel().getSelectionModel().clearSelection();
      }
    });
    getEditModel().eventAfterDelete().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> entities = ((DeleteEvent) e).getDeletedEntities();
        if (containsTableModel())
          getTableModel().removeItems(entities);

        refreshDetailModelsAfterDelete(entities);
      }
    });
    evtLinkedDetailModelsChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (!getEditModel().isEntityNull())
          updateDetailModelsByActiveEntity();
      }
    });
    if (!containsTableModel()) {
      getEditModel().eventEntityChanged().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent event) {
          updateDetailModelsByActiveEntity();
        }
      });
    }
  }

  private void bindTableModelEventsInternal() {
    if (!containsTableModel())
      return;

    getTableModel().eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        updateDetailModelsByActiveEntity();
      }
    });
  }
}