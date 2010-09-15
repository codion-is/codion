/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.DeleteListener;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.InsertListener;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.event.UpdateListener;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A default EntityModel implementation.
 *
 * <pre>
 * String entityID = "some.entity";
 * String clientTypeID = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityDbProvider dbProvider = EntityDbProviderFactory.createEntityDbProvider(user, clientTypeID);
 *
 * EntityModel model = new DefaultEntityModel(entityID, dbProvider);
 *
 * EntityPanel panel = new EntityPanel(model);
 * </pre>
 */
public class DefaultEntityModel implements EntityModel {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModel.class);

  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshDone = Events.event();
  private final Event evtLinkedDetailModelsChanged = Events.event();

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
  public DefaultEntityModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, true);
  }

  /**
   * Instantiates a new DefaultEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityID the ID of the Entity this DefaultEntityModel represents
   * @param dbProvider a EntityDbProvider
   * @param includeTableModel true if this DefaultEntityModel should include a table model
   */
  public DefaultEntityModel(final String entityID, final EntityDbProvider dbProvider, final boolean includeTableModel) {
    Util.rejectNullValue(dbProvider, "dbProvider");
    Util.rejectNullValue(entityID, "entityID");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.editModel = new DefaultEntityEditModel(entityID, dbProvider);
    if (includeTableModel) {
      this.tableModel = new DefaultEntityTableModel(entityID, dbProvider);
    }
    else {
      this.tableModel = null;
    }
    if (tableModel != null) {
      tableModel.setEditModel(editModel);
    }
    bindEvents();
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   */
  public DefaultEntityModel(final EntityEditModel editModel) {
    this(editModel, true);
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param tableModel the table model
   */
  public DefaultEntityModel(final EntityTableModel tableModel) {
    this(new DefaultEntityEditModel(tableModel.getEntityID(), tableModel.getDbProvider()), tableModel);
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   * @param includeTableModel if true then a default EntityTableModel is included
   */
  public DefaultEntityModel(final EntityEditModel editModel, final boolean includeTableModel) {
    this(editModel, includeTableModel ? new DefaultEntityTableModel(editModel.getEntityID(), editModel.getDbProvider()) : null);
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public DefaultEntityModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    Util.rejectNullValue(editModel, "editModel");
    this.entityID = editModel.getEntityID();
    this.dbProvider = editModel.getDbProvider();
    this.editModel = editModel;
    this.tableModel = tableModel;
    if (tableModel != null) {
      tableModel.setEditModel(editModel);
    }
    bindEvents();
  }

  /**
   * @return a String representation of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityID;
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
  public final EntityModel getMasterModel() {
    return masterModel;
  }

  /** {@inheritDoc} */
  public final void setMasterModel(final EntityModel entityModel) {
    this.masterModel = entityModel;
  }

  /** {@inheritDoc} */
  public final EntityEditModel getEditModel() {
    return editModel;
  }

  /** {@inheritDoc} */
  public final EntityTableModel getTableModel() {
    return tableModel;
  }

  /** {@inheritDoc} */
  public final boolean containsTableModel() {
    return tableModel != null;
  }

  /** {@inheritDoc} */
  public final void addDetailModels(final EntityModel... detailModels) {
    Util.rejectNullValue(detailModels, "detailModels");
    for (final EntityModel detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  /** {@inheritDoc} */
  public final EntityModel addDetailModel(final EntityModel detailModel) {
    this.detailModels.add(detailModel);
    detailModel.setMasterModel(this);
    if (detailModel.containsTableModel()) {
      detailModel.getTableModel().setDetailModel(true);
    }

    return detailModel;
  }

  /** {@inheritDoc} */
  public final boolean containsDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  public final boolean containsDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  public final Collection<? extends EntityModel> getDetailModels() {
    return Collections.unmodifiableCollection(detailModels);
  }

  /** {@inheritDoc} */
  public final void setLinkedDetailModels(final EntityModel... detailModels) {
    final Set<EntityModel> linked = new HashSet<EntityModel>(linkedDetailModels);
    linkedDetailModels.clear();
    if (detailModels != null) {
      for (final EntityModel detailModel : detailModels) {
        if (detailModel != null) {
          linkedDetailModels.add(detailModel);
        }
      }
    }

    if (!linkedDetailModels.equals(linked)) {
      evtLinkedDetailModelsChanged.fire();
    }
  }

  /** {@inheritDoc} */
  public final Collection<EntityModel> getLinkedDetailModels() {
    return Collections.unmodifiableCollection(linkedDetailModels);
  }

  /** {@inheritDoc} */
  public final EntityModel getDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return detailModel;
      }
    }

    if (Configuration.getBooleanValue(Configuration.AUTO_CREATE_ENTITY_MODELS)) {
      try {
        final EntityModel detailModel = modelClass.getConstructor(EntityDbProvider.class).newInstance(dbProvider);
        addDetailModel(detailModel);
        return detailModel;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException("No detail model of type " + modelClass + " found in model: " + this);
  }

  /** {@inheritDoc} */
  public final EntityModel getDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return detailModel;
      }
    }

    if (Configuration.getBooleanValue(Configuration.AUTO_CREATE_ENTITY_MODELS)) {
      final EntityModel detailModel = new DefaultEntityModel(entityID, dbProvider);
      addDetailModel(detailModel);
      return detailModel;
    }
    throw new IllegalArgumentException("No detail model for type " + entityID + " found in model: " + this);
  }

  /** {@inheritDoc} */
  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      LOG.debug(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      if (containsTableModel()) {
        tableModel.refresh();
      }
      initializeDetailModels();
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      LOG.debug(this + " done refreshing");
    }
  }

  /** {@inheritDoc} */
  public final void refreshDetailModels() {
    for (final EntityModel detailModel : detailModels) {
      detailModel.refresh();
    }
  }

  /** {@inheritDoc} */
  public final void clear() {
    if (containsTableModel()) {
      tableModel.clear();
    }
    editModel.clear();
    clearDetailModels();
  }

  /** {@inheritDoc} */
  public final void clearDetailModels() {
    for (final EntityModel detailModel : detailModels) {
      detailModel.clear();
    }
  }

  /** {@inheritDoc} */
  public void initialize(final String masterEntityID, final List<Entity> selectedMasterEntities) {
    if (containsTableModel()) {
      tableModel.setForeignKeySearchValues(masterEntityID, selectedMasterEntities);
    }

    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID, masterEntityID)) {
      editModel.setValue(foreignKeyProperty.getPropertyID(), selectedMasterEntities != null
              && !selectedMasterEntities.isEmpty() ? selectedMasterEntities.get(0) : null);
    }
    handleInitialization(masterEntityID, selectedMasterEntities);
  }

  /** {@inheritDoc} */
  public final void addLinkedDetailModelsListener(final ActionListener listener) {
    evtLinkedDetailModelsChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeLinkedDetailModelsListener(final ActionListener listener) {
    evtLinkedDetailModelsChanged.removeListener(listener);
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
   * @param masterEntityID the entity ID of the master model doing the initialization
   * @param selectedMasterEntities the entities selected in the master model
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleInitialization(final String masterEntityID, final List<Entity> selectedMasterEntities) {}

  private void initializeDetailModels() {
    final List<Entity> activeEntities = getActiveEntities();
    for (final EntityModel detailModel : linkedDetailModels) {
      detailModel.initialize(entityID, activeEntities);
    }
  }

  private void handleInsert(final InsertEvent insertEvent) {
    final List<Entity.Key> primaryKeys = insertEvent.getInsertedKeys();
    if (containsTableModel()) {
      tableModel.clearSelection();
      tableModel.addEntitiesByPrimaryKeys(primaryKeys, true);
    }

    refreshDetailModelsAfterInsert(primaryKeys);
  }

  private void handleUpdate(final UpdateEvent updateEvent) {
    final List<Entity> updatedEntities = updateEvent.getUpdatedEntities();
    if (containsTableModel()) {
      if (updateEvent.isPrimaryKeyModified()) {
        tableModel.refresh();//best we can do under the circumstances
      }
      else {//replace the updated entities in the table model
        final List<Entity> updated = new ArrayList<Entity>();
        for (final Entity entity : updatedEntities) {
          if (entity.is(entityID)) {
            updated.add(entity);
          }
        }
        tableModel.replaceEntities(updated);
      }
    }

    refreshDetailModelsAfterUpdate(updatedEntities);
  }

  private void handleDelete(final DeleteEvent deleteEvent) {
    refreshDetailModelsAfterDelete(deleteEvent.getDeletedEntities());
  }

  /**
   * Removes the deleted entities from combobox models
   * @param deletedEntities the deleted entities
   */
  private void refreshDetailModelsAfterDelete(final List<Entity> deletedEntities) {
    if (deletedEntities.isEmpty()) {
      return;
    }

    for (final EntityModel detailModel : detailModels) {
      for (final Property.ForeignKeyProperty foreignKeyProperty :
              Entities.getForeignKeyProperties(detailModel.getEntityID(), entityID)) {
        final EntityEditModel detailEditModel = detailModel.getEditModel();
        if (detailEditModel.containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
          final EntityComboBoxModel comboModel = detailEditModel.getEntityComboBoxModel(foreignKeyProperty);
          final Entity selectedEntity = comboModel.getSelectedEntity();
          for (final Entity deletedEntity : deletedEntities) {
            comboModel.removeItem(deletedEntity);
          }
          if (comboModel.isVisible(selectedEntity)) {
            comboModel.setSelectedItem(selectedEntity);
          }
          else if (comboModel.getSize() > 0) {
            comboModel.setSelectedItem(comboModel.getElementAt(0));
          }
          else {
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
  private void refreshDetailModelsAfterInsert(final List<Entity.Key> insertedPrimaryKeys) {
    if (detailModels.isEmpty()) {
      return;
    }

    try {
      final Entity insertedEntity = dbProvider.getEntityDb().selectSingle(insertedPrimaryKeys.get(0));
      for (final EntityModel detailModel : detailModels) {
        for (final Property.ForeignKeyProperty foreignKeyProperty :
                Entities.getForeignKeyProperties(detailModel.getEntityID(), entityID)) {
          final EntityEditModel detailEditModel = detailModel.getEditModel();
          if (detailEditModel.containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
            detailEditModel.getEntityComboBoxModel(foreignKeyProperty).refresh();
          }
          detailEditModel.setValue(foreignKeyProperty.getPropertyID(), insertedEntity);
        }
      }
    }
    catch (DbException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  private void refreshDetailModelsAfterUpdate(final Collection<Entity> updatedEntities) {
    for (final EntityModel detailModel : detailModels) {
      detailModel.getEditModel().replaceForeignKeyValues(entityID, updatedEntities);
      if (detailModel.containsTableModel()) {
        detailModel.getTableModel().replaceForeignKeyValues(entityID, updatedEntities);
      }
    }
  }

  private List<Entity> getActiveEntities() {
    final List<Entity> activeEntities;
    if (containsTableModel()) {
      if (tableModel.isSelectionEmpty()) {
        activeEntities = null;
      }
      else {
        activeEntities = tableModel.getSelectedItems();
      }
    }
    else {
      if (editModel.isEntityNew()) {
        activeEntities = null;
      }
      else {
        activeEntities = Arrays.asList(editModel.getEntityCopy());
      }
    }
    return activeEntities;
  }

  private void bindEvents() {
    editModel.addAfterInsertListener(new InsertListener() {
      /** {@inheritDoc} */
      @Override
      public void inserted(final InsertEvent event) {
        handleInsert(event);
      }
    });
    editModel.addAfterUpdateListener(new UpdateListener() {
      /** {@inheritDoc} */
      @Override
      protected void updated(final UpdateEvent event) {
        handleUpdate(event);
      }
    });
    editModel.addAfterDeleteListener(new DeleteListener() {
      /** {@inheritDoc} */
      @Override
      protected void deleted(final DeleteEvent event) {
        handleDelete(event);
      }
    });
    final ActionListener initializer = new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        if (!editModel.isEntityNew()) {
          initializeDetailModels();
        }
      }
    };
    evtLinkedDetailModelsChanged.addListener(initializer);
    if (containsTableModel()) {
      tableModel.addSelectionChangedListener(initializer);
    }
    else {
      editModel.addValueMapSetListener(initializer);
    }
  }
}