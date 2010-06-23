/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.reporting.EntityReportUtil;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import org.apache.log4j.Logger;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class responsible for, among other things, coordinating a EntityEditModel and an EntityTableModel.
 */
public class EntityModel {

  protected static final Logger LOG = Util.getLogger(EntityModel.class);

  private final Event evtEntitiesChanged = new Event();
  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();
  private final Event evtLinkedDetailModelsChanged = new Event();
  private final State stCascadeRefresh = new State();

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The EntityEditModel instance
   */
  private final EntityEditModel editModel;

  /**
   * The table model
   */
  private final EntityTableModel tableModel;

  /**
   * The EntityDb connection provider
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final List<EntityModel> detailModels = new ArrayList<EntityModel>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final Set<EntityModel> linkedDetailModels = new HashSet<EntityModel>();

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
   * Instantiates a new EntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a EntityDbProvider
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, true);
  }

  /**
   * Instantiates a new EntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityID the ID of the Entity this EntityModel represents
   * @param dbProvider a EntityDbProvider
   * @param includeTableModel true if this EntityModel should include a table model
   */
  public EntityModel(final String entityID, final EntityDbProvider dbProvider, final boolean includeTableModel) {
    Util.rejectNullValue(dbProvider);
    Util.rejectNullValue(entityID);
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.editModel = new EntityEditModel(entityID, dbProvider);
    if (includeTableModel) {
      this.tableModel = new EntityTableModel(entityID, dbProvider);
    }
    else {
      this.tableModel = null;
    }
    if (tableModel != null) {
      tableModel.setEditModel(editModel);
    }
    initializeAssociatedModels();
    bindEventsInternal();
    bindEvents();
    bindTableModelEventsInternal();
    bindTableModelEvents();
  }

  /**
   * Instantiates a new EntityModel
   * @param editModel the edit model
   */
  public EntityModel(final EntityEditModel editModel) {
    this(editModel, true);
  }

  /**
   * Instantiates a new EntityModel
   * @param tableModel the table model
   */
  public EntityModel(final EntityTableModel tableModel) {
    this(new EntityEditModel(tableModel.getEntityID(), tableModel.getDbProvider()), tableModel);
  }

  /**
   * Instantiates a new EntityModel
   * @param editModel the edit model
   * @param includeTableModel if true then a default EntityTableModel is included
   */
  public EntityModel(final EntityEditModel editModel, final boolean includeTableModel) {
    this(editModel, includeTableModel ? new EntityTableModel(editModel.getEntityID(), editModel.getDbProvider()) : null);
  }

  /**
   * Instantiates a new EntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public EntityModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    Util.rejectNullValue(editModel);
    this.entityID = editModel.getEntityID();
    this.dbProvider = editModel.getDbProvider();
    this.editModel = editModel;
    this.tableModel = tableModel;
    if (tableModel != null) {
      tableModel.setEditModel(editModel);
    }
    initializeAssociatedModels();
    bindEventsInternal();
    bindEvents();
    bindTableModelEventsInternal();
    bindTableModelEvents();
  }

  /**
   * Adds the given detail models to this model.
   * @param detailModels the detail models to add
   */
  public void addDetailModels(final EntityModel... detailModels) {
    Util.rejectNullValue(detailModels);
    for (final EntityModel detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  /**
   * Adds the given detail model to this model
   * @param detailModel the detail model
   * @return the detail model just added
   */
  public EntityModel addDetailModel(final EntityModel detailModel) {
    this.detailModels.add(detailModel);
    detailModel.masterModel = this;
    if (detailModel.containsTableModel()) {
      detailModel.tableModel.setDetailModel(true);
    }

    return detailModel;
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
    for (final EntityModel detailModel : detailModels) {
      detailModel.setCascadeRefresh(value);
    }

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
   * @see EntityTableModel#searchByForeignKeyValues(String, java.util.List)
   */
  public void setSelectionFiltersDetail(final boolean value) {
    for (final EntityModel detailModel : detailModels) {
      detailModel.setSelectionFiltersDetail(value);
    }
    if (containsTableModel()) {
      tableModel.getSelectionModel().clearSelection();
    }
    selectionFiltersDetail = value;
  }

  /**
   * @return the master model, if any
   */
  public EntityModel getMasterModel() {
    return masterModel;
  }

  /**
   * @return the EntityEditModel instance used by this ValueChangeMapModel
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
   * @return true if this ValueChangeMapModel contains a TableModel
   */
  public boolean containsTableModel() {
    return tableModel != null;
  }

  /**
   * @param modelClass the detail model class
   * @return true if this model contains a detail model of the given class
   */
  public boolean containsDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /**
   * @return the detail models this model contains
   */
  public List<? extends EntityModel> getDetailModels() {
    return Collections.unmodifiableList(detailModels);
  }

  /**
   * @return the linked detail model or the first detail model if none is linked,
   * returns null in case this model contains no detail models
   */
  public EntityModel getLinkedDetailModel() {
    if (detailModels.size() == 0) {
      return null;
    }

    return linkedDetailModels.size() > 0 ? linkedDetailModels.iterator().next() : detailModels.get(0);
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
    final Set<EntityModel> linked = new HashSet<EntityModel>(linkedDetailModels);
    linkedDetailModels.clear();
    if (detailModels != null) {
      linkedDetailModels.addAll(detailModels);
    }

    if (!linkedDetailModels.equals(linked)) {
      evtLinkedDetailModelsChanged.fire();
    }
  }

  /**
   * @return a collection containing the detail models that are currently linked to this model
   */
  public Collection<EntityModel> getLinkedDetailModels() {
    return Collections.unmodifiableCollection(linkedDetailModels);
  }

  /**
   * Returns the first detail model of the given type
   * @param entityModelClass the type of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  public EntityModel getDetailModel(final Class<? extends EntityModel> entityModelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(entityModelClass)) {
        return detailModel;
      }
    }

    throw new RuntimeException("No detail model of type " + entityModelClass + " found in model: " + this);
  }

  /**
   * Returns an initialized ReportResult object from the given report wrapper
   * @param reportWrapper the report wrapper
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public ReportResult fillReport(final ReportWrapper reportWrapper, final Map reportParameters) throws ReportException {
    return EntityReportUtil.fillReport(reportWrapper, dbProvider, reportParameters);
  }

  /**
   * Returns an initialized ReportResult object from the given report wrapper and data wrapper
   * @param reportWrapper the report wrapper
   * @param dataSource the ReportDataWrapper used to provide the report data
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  public ReportResult fillReport(final ReportWrapper reportWrapper, final ReportDataWrapper dataSource,
                                 final Map reportParameters) throws ReportException {
    return EntityReportUtil.fillReport(reportWrapper, dataSource, reportParameters);
  }

  /**
   * Refreshes this EntityModel
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   * @see #isCascadeRefresh
   */
  public void refresh() {
    if (isRefreshing) {
      return;
    }

    try {
      LOG.trace(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      editModel.refresh();//triggers table model refresh as per bindTableModelEventsInternal()
      if (isCascadeRefresh()) {
        refreshDetailModels();
      }

      updateDetailModelsByActiveEntity();
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      LOG.trace(this + " done refreshing");
    }
  }

  public void clear() {
    if (containsTableModel()) {
      tableModel.clear();
    }
    editModel.clear();
    clearDetailModels();
  }

  /**
   * Updates this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterEntityID the ID of the master entity
   * @param selectedMasterEntities the master entities
   */
  public void masterSelectionChanged(final String masterEntityID, final List<Entity> selectedMasterEntities) {
    if (selectionFiltersDetail && containsTableModel()) {
      tableModel.searchByForeignKeyValues(masterEntityID, selectedMasterEntities);
    }

    for (final Property.ForeignKeyProperty foreignKeyProperty : EntityRepository.getForeignKeyProperties(entityID, masterEntityID)) {
      editModel.setValue(foreignKeyProperty.getPropertyID(), selectedMasterEntities != null && selectedMasterEntities.size() > 0 ? selectedMasterEntities.get(0) : null);
    }
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
   * Override this method to initialize any associated models before bindEvents() is called.
   * An associated model could for example be an EntityModel that is used by this model but is not a detail model.
   */
  protected void initializeAssociatedModels() {}

  protected void handleInsert(final InsertEvent insertEvent) {
    final List<Entity.Key> primaryKeys = insertEvent.getInsertedKeys();
    if (containsTableModel()) {
      tableModel.getSelectionModel().clearSelection();
      tableModel.addEntitiesByPrimaryKeys(primaryKeys, true);
    }

    refreshDetailModelsAfterInsert(primaryKeys);
  }

  protected void handleUpdate(final UpdateEvent updateEvent) {
    final List<Entity> updatedEntities = updateEvent.getUpdatedEntities();
    if (containsTableModel()) {
      if (updateEvent.isPrimaryKeyModified()) {
        tableModel.refresh();//best we can do under the circumstances
      }
      else {//replace and select the updated entities
        final List<Entity> updated = new ArrayList<Entity>();
        for (final Entity entity : updatedEntities) {
          if (entity.is(entityID)) {
            updated.add(entity);
          }
        }
        tableModel.replaceEntities(updated);
        tableModel.setSelectedItems(updated);
      }
    }

    refreshDetailModelsAfterUpdate(updatedEntities);
  }

  protected void handleDelete(final DeleteEvent deleteEvent) {
    refreshDetailModelsAfterDelete(deleteEvent.getDeletedEntities());
  }

  /**
   * Removes the deleted entities from combobox models
   * @param deletedEntities the deleted entities
   */
  protected void refreshDetailModelsAfterDelete(final List<Entity> deletedEntities) {
    if (deletedEntities.size() > 0) {
      for (final EntityModel detailModel : detailModels) {
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.entityID, entityID)) {
          final EntityEditModel detailEditModel = detailModel.editModel;
          if (detailEditModel.containsComboBoxModel(foreignKeyProperty)) {
            final EntityComboBoxModel comboModel = detailEditModel.getEntityComboBoxModel(foreignKeyProperty);
            for (final Entity deletedEntity : deletedEntities) {
              comboModel.removeItem(deletedEntity);
            }
            if (comboModel.getSize() > 0) {
              comboModel.setSelectedItem(comboModel.getElementAt(0));
            }
            else {
              comboModel.setSelectedItem(null);
            }
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
    if (detailModels.size() == 0) {
      return;
    }

    try {
      final Entity insertedEntity = getEntityDb().selectSingle(insertedPrimaryKeys.get(0));
      for (final EntityModel detailModel : detailModels) {
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                EntityRepository.getForeignKeyProperties(detailModel.entityID, entityID)) {
          final EntityEditModel detailEditModel = detailModel.editModel;
          if (detailEditModel.containsComboBoxModel(foreignKeyProperty)) {
            detailEditModel.getEntityComboBoxModel(foreignKeyProperty).refresh();
          }
          detailEditModel.setValue(foreignKeyProperty.getPropertyID(), insertedEntity);
        }
      }
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void refreshDetailModelsAfterUpdate(final List<Entity> entities) {
    for (final EntityModel detailModel : detailModels) {
      for (final Property.ForeignKeyProperty foreignKeyProperty :
              EntityRepository.getForeignKeyProperties(detailModel.entityID, entityID)) {
        final EntityEditModel detailEditModel = detailModel.editModel;
        if (detailEditModel.containsComboBoxModel(foreignKeyProperty)) {
          detailEditModel.getEntityComboBoxModel(foreignKeyProperty).refresh();
        }
      }
    }
  }

  protected void refreshDetailModels() {
    LOG.trace(this + " refreshing detail models");
    for (final EntityModel detailModel : detailModels) {
      detailModel.refresh();
    }
  }

  protected void clearDetailModels() {
    LOG.trace(this + " clearing detail models");
    for (final EntityModel detailModel : detailModels) {
      detailModel.clear();
    }
  }

  protected void updateDetailModelsByActiveEntity() {
    final List<Entity> activeEntities = containsTableModel() ?
            (tableModel.stateSelectionEmpty().isActive() ? null : tableModel.getSelectedItems()) :
            (editModel.isEntityNew() ? null : Arrays.asList(editModel.getEntityCopy()));
    for (final EntityModel detailModel : linkedDetailModels) {
      detailModel.masterSelectionChanged(entityID, activeEntities);
    }
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Override to add specific event bindings that depend on the table model
   */
  protected void bindTableModelEvents() {}

  private void bindEventsInternal() {
    editModel.eventAfterInsert().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleInsert((InsertEvent) e);
      }
    });
    editModel.eventAfterUpdate().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleUpdate((UpdateEvent) e);
      }
    });
    editModel.eventAfterDelete().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleDelete((DeleteEvent) e);
      }
    });
    evtLinkedDetailModelsChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!getEditModel().isEntityNew()) {
          updateDetailModelsByActiveEntity();
        }
      }
    });
    if (!containsTableModel()) {
      editModel.eventValueMapSet().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          updateDetailModelsByActiveEntity();
        }
      });
    }
  }

  private void bindTableModelEventsInternal() {
    if (!containsTableModel()) {
      return;
    }

    editModel.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tableModel.refresh();
      }
    });
    tableModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateDetailModelsByActiveEntity();
      }
    });
    tableModel.eventSelectedIndexChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        editModel.setValueMap(tableModel.getSelectionModel().isSelectionEmpty() ? null : tableModel.getSelectedItem());
      }
    });

    tableModel.addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent e) {
        //if the selected record is being updated via the table model refresh the one in the edit model
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == tableModel.getSelectedIndex()) {
          editModel.setValueMap(null);
          editModel.setValueMap(tableModel.getSelectedItem());
        }
      }
    });
  }
}