/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.DeleteListener;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.InsertListener;
import org.jminor.framework.client.model.event.UpdateEvent;
import org.jminor.framework.client.model.event.UpdateListener;
import org.jminor.framework.db.provider.EntityConnectionProvider;
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
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeID);
 *
 * EntityModel model = new DefaultEntityModel(entityID, connectionProvider);
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
   * The EntityTableModel model
   */
  private final EntityTableModel tableModel;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final Collection<EntityModel> detailModels = new ArrayList<EntityModel>();

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
   * Instantiates a new DefaultEntityModel with default EntityEditModel and EntityTableModel implementations.
   * @param entityID the ID of the Entity this DefaultEntityModel represents
   * @param connectionProvider a EntityConnectionProvider
   */
  public DefaultEntityModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(new DefaultEntityEditModel(Util.rejectNullValue(entityID, "entityID"),
            Util.rejectNullValue(connectionProvider, "connectionProvider")));
  }

  /**
   * Instantiates a new DefaultEntityModel, including a default {@link EntityTableModel}
   * @param editModel the edit model
   */
  public DefaultEntityModel(final EntityEditModel editModel) {
    this(editModel, new DefaultEntityTableModel(editModel.getEntityID(), editModel.getConnectionProvider()));
  }

  /**
   * Instantiates a new DefaultEntityModel, including a default {@link EntityEditModel}
   * @param tableModel the table model
   */
  public DefaultEntityModel(final EntityTableModel tableModel) {
    this(new DefaultEntityEditModel(tableModel.getEntityID(), tableModel.getConnectionProvider()), tableModel);
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public DefaultEntityModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    Util.rejectNullValue(editModel, "editModel");
    this.entityID = editModel.getEntityID();
    this.connectionProvider = editModel.getConnectionProvider();
    this.editModel = editModel;
    this.tableModel = tableModel;
    setTableEditModel(editModel, tableModel);
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
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
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
    if (this.detailModels.contains(detailModel)) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already been added");
    }
    this.detailModels.add(detailModel);
    detailModel.setMasterModel(this);
    if (detailModel.containsTableModel()) {
      detailModel.getTableModel().setQueryCriteriaRequired(true);
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
  public final boolean containsDetailModel(final EntityModel detailModel) {
    return detailModels.contains(detailModel);
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

    return null;
  }

  /** {@inheritDoc} */
  public final EntityModel getDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityID + " found in model: " + this);
  }

  /** {@inheritDoc} */
  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      LOG.debug("{} refreshing", this);
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
      LOG.debug("{} done refreshing", this);
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
  public final void initialize(final String foreignKeyEntityID, final List<Entity> foreignKeyValues) {
    if (containsTableModel()) {
      tableModel.setForeignKeySearchValues(foreignKeyEntityID, foreignKeyValues);
    }

    if (editModel.isEntityNew()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID, foreignKeyEntityID)) {
        final Entity referencedEntity = foreignKeyValues == null || foreignKeyValues.isEmpty() ?
                null : foreignKeyValues.get(0);
        editModel.setValue(foreignKeyProperty.getPropertyID(), referencedEntity);
      }
    }
    handleInitialization(foreignKeyEntityID, foreignKeyValues);
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
   * @param foreignKeyEntityID the entity ID of the foreign key referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleInitialization(final String foreignKeyEntityID, final List<Entity> foreignKeyValues) {}

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
          final Entity selectedEntity = comboModel.getSelectedValue();
          for (final Entity deletedEntity : deletedEntities) {
            comboModel.removeItem(deletedEntity);
          }
          if (comboModel.isVisible(selectedEntity)) {
            comboModel.setSelectedItem(selectedEntity);
          }//if the null value is selected we're fine, otherwise select topmost item
          else if (!comboModel.isNullValueSelected() && comboModel.getSize() > 0) {
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
      final Entity insertedEntity = connectionProvider.getConnection().selectSingle(insertedPrimaryKeys.get(0));
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
    catch (DatabaseException ex) {
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
        activeEntities = Collections.emptyList();
      }
      else {
        activeEntities = tableModel.getSelectedItems();
      }
    }
    else {
      if (editModel.isEntityNew()) {
        activeEntities = Collections.emptyList();
      }
      else {
        activeEntities = Arrays.asList(editModel.getEntityCopy());
      }
    }
    return activeEntities;
  }

  private void setTableEditModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    if (tableModel != null) {
      if (!entityID.equals(tableModel.getEntityID())) {
        throw new IllegalArgumentException("Table model entityID mismatch, found: " + tableModel.getEntityID() + ", required: " + entityID);
      }
    }
    if (tableModel != null) {
      if (tableModel.hasEditModel()) {
        if (tableModel.getEditModel() != editModel) {
          throw new IllegalArgumentException("Edit model type or instance mismatch, found: " + tableModel.getEditModel() + ", required: " + editModel);
        }
      }
      else {
        tableModel.setEditModel(editModel);
      }
    }
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
        initializeDetailModels();
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