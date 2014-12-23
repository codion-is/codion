/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  private final Event refreshStartedEvent = Events.event();
  private final Event refreshDoneEvent = Events.event();
  private final Event linkedDetailModelsChangedEvent = Events.event();

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
  private final Collection<EntityModel> detailModels = new ArrayList<>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final Set<EntityModel> linkedDetailModels = new HashSet<>();

  /**
   * Maps detail models to the foreign key property they are based on
   */
  private final Map<EntityModel, Property.ForeignKeyProperty> detailModelForeignKeys = new HashMap<>();

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
    this(tableModel.hasEditModel() ? tableModel.getEditModel() : new DefaultEntityEditModel(tableModel.getEntityID(),
            tableModel.getConnectionProvider()), tableModel);
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
  public final EntityModel getMasterModel() {
    return masterModel;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMasterModel(final EntityModel entityModel) {
    if (this.masterModel != null) {
      throw new IllegalStateException("Master model has already been set for " + this);
    }
    this.masterModel = entityModel;
    bindMasterModelEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel getEditModel() {
    return editModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel getTableModel() {
    return tableModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsTableModel() {
    return tableModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final void addDetailModels(final EntityModel... detailModels) {
    Util.rejectNullValue(detailModels, "detailModels");
    for (final EntityModel detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel addDetailModel(final EntityModel detailModel) {
    if (this.detailModels.contains(detailModel)) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already been added");
    }
    if (detailModel.getMasterModel() != null) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already had a master model defined");
    }
    this.detailModels.add(detailModel);
    detailModel.setMasterModel(this);
    if (detailModel.containsTableModel()) {
      detailModel.getTableModel().setQueryCriteriaRequired(true);
    }

    return detailModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final EntityModel detailModel) {
    return detailModels.contains(detailModel);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<? extends EntityModel> getDetailModels() {
    return Collections.unmodifiableCollection(detailModels);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLinkedDetailModel(final EntityModel detailModel) {
    if (detailModel != null && linkedDetailModels.add(detailModel)) {
      linkedDetailModelsChangedEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLinkedDetailModel(final EntityModel detailModel) {
    if (detailModel != null && linkedDetailModels.remove(detailModel)) {
      linkedDetailModelsChangedEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<EntityModel> getLinkedDetailModels() {
    return Collections.unmodifiableCollection(linkedDetailModels);
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel getDetailModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return detailModel;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel getDetailModel(final String entityID) {
    for (final EntityModel detailModel : detailModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityID + " found in model: " + this);
  }

  /** {@inheritDoc} */
  @Override
  public final void setDetailModelForeignKey(final EntityModel detailModel, final String foreignKeyPropertyID) {
    Util.rejectNullValue(detailModel, "detailModel");
    if (!containsDetailModel(detailModel)) {
      throw new IllegalArgumentException(this + " does not contain detail model: " + detailModel);
    }

    if (foreignKeyPropertyID == null) {
      detailModelForeignKeys.remove(detailModel);
    }
    else {
      detailModelForeignKeys.put(detailModel, Entities.getForeignKeyProperty(detailModel.getEntityID(), foreignKeyPropertyID));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      LOG.debug("{} refreshing", this);
      isRefreshing = true;
      refreshStartedEvent.fire();
      if (containsTableModel()) {
        tableModel.refresh();
      }
      initializeDetailModels();
    }
    finally {
      isRefreshing = false;
      refreshDoneEvent.fire();
      LOG.debug("{} done refreshing", this);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void refreshDetailModels() {
    for (final EntityModel detailModel : detailModels) {
      detailModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    if (containsTableModel()) {
      tableModel.clear();
    }
    editModel.clear();
    clearDetailModels();
  }

  /** {@inheritDoc} */
  @Override
  public final void clearDetailModels() {
    for (final EntityModel detailModel : detailModels) {
      detailModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void initialize(final String foreignKeyEntityID, final List<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(entityID, foreignKeyEntityID);
    if (!foreignKeyProperties.isEmpty()) {
      initialize(foreignKeyProperties.get(0), foreignKeyValues);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void initialize(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (containsTableModel()) {
      tableModel.setForeignKeySearchValues(foreignKeyProperty, foreignKeyValues);
    }

    if (editModel.isEntityNew() && !Util.nullOrEmpty(foreignKeyValues)) {
      editModel.setValue(foreignKeyProperty.getPropertyID(), foreignKeyValues.get(0));
    }
    handleInitialization(foreignKeyProperty, foreignKeyValues);
  }

  /** {@inheritDoc} */
  @Override
  public void savePreferences() {
    if (containsTableModel()) {
      getTableModel().savePreferences();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addLinkedDetailModelsListener(final EventListener listener) {
    linkedDetailModelsChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLinkedDetailModelsListener(final EventListener listener) {
    linkedDetailModelsChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addBeforeRefreshListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  /**
   * @param foreignKeyProperty the foreign key referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleInitialization(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {}

  /**
   * Initializes all linked detail models according to the active entities in this master model
   * @see #getActiveEntities()
   * @see #addLinkedDetailModel(EntityModel)
   * @see #initialize(org.jminor.framework.domain.Property.ForeignKeyProperty, java.util.List)
   * @see #initialize(String, java.util.List)
   */
  protected final void initializeDetailModels() {
    final List<Entity> activeEntities = getActiveEntities();
    for (final EntityModel detailModel : linkedDetailModels) {
      if (detailModelForeignKeys.containsKey(detailModel)) {
        detailModel.initialize(detailModelForeignKeys.get(detailModel), activeEntities);
      }
      else {
        detailModel.initialize(entityID, activeEntities);
      }
    }
  }

  /**
   * Adds the inserted entities to the EntityComboBoxModels based on the inserted entity type
   * and sets the value of the master foreign key property
   * @param insertEvent the insert event
   */
  private void handleMasterInsert(final EntityEditModel.InsertEvent insertEvent) {
    for (final Property.ForeignKeyProperty foreignKeyProperty :
            Entities.getForeignKeyProperties(getEntityID(), masterModel.getEntityID())) {
      if (editModel.containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
        for (final Entity entity : insertEvent.getInsertedEntities()) {
          editModel.getEntityComboBoxModel(foreignKeyProperty).addItem(entity);
        }
      }//todo problematic with multiple foreign keys to the same entity, masterModelForeignKeys?
      editModel.setValue(foreignKeyProperty.getPropertyID(), insertEvent.getInsertedEntities().get(0));
    }
  }

  /**
   * Replaces the updated master entities wherever they are referenced
   * @param updateEvent the update event
   */
  private void handleMasterUpdate(final EntityEditModel.UpdateEvent updateEvent) {
    editModel.replaceForeignKeyValues(masterModel.getEntityID(), updateEvent.getUpdatedEntities().values());
    if (containsTableModel()) {
      getTableModel().replaceForeignKeyValues(masterModel.getEntityID(), updateEvent.getUpdatedEntities().values());
    }
  }

  /**
   * Removes the deleted entities from all ComboBox models based on that entity type
   * @param deleteEvent the delete event
   */
  private void handleMasterDelete(final EntityEditModel.DeleteEvent deleteEvent) {
    if (deleteEvent.getDeletedEntities().isEmpty()) {
      return;
    }

    for (final Property.ForeignKeyProperty foreignKeyProperty :
            Entities.getForeignKeyProperties(getEntityID(), masterModel.getEntityID())) {
      if (editModel.containsComboBoxModel(foreignKeyProperty.getPropertyID())) {
        final EntityComboBoxModel comboModel = editModel.getEntityComboBoxModel(foreignKeyProperty);
        final Entity selectedEntity = comboModel.getSelectedValue();
        for (final Entity deletedEntity : deleteEvent.getDeletedEntities()) {
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

  private List<Entity> getActiveEntities() {
    final List<Entity> activeEntities;
    if (containsTableModel() && !tableModel.getSelectionModel().isSelectionEmpty()) {
      activeEntities = tableModel.getSelectionModel().getSelectedItems();
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
    if (tableModel != null && !entityID.equals(tableModel.getEntityID())) {
      throw new IllegalArgumentException("Table model entityID mismatch, found: " + tableModel.getEntityID() + ", required: " + entityID);
    }
    if (tableModel != null) {
      if (tableModel.hasEditModel()) {
        if (tableModel.getEditModel() != editModel) {
          throw new IllegalArgumentException("Edit model instance mismatch, found: " + tableModel.getEditModel() + ", required: " + editModel);
        }
      }
      else {
        tableModel.setEditModel(editModel);
      }
    }
  }

  private void bindEvents() {
    final EventListener initializer = new EventListener() {
      @Override
      public void eventOccurred() {
        initializeDetailModels();
      }
    };
    linkedDetailModelsChangedEvent.addListener(initializer);
    if (containsTableModel()) {
      tableModel.getSelectionModel().addSelectionChangedListener(initializer);
    }
    else {
      editModel.addEntitySetListener(new EventInfoListener<Entity>() {
        @Override
        public void eventOccurred(final Entity info) {
          initializeDetailModels();
        }
      });
    }
  }

  private void bindMasterModelEvents() {
    masterModel.getEditModel().addAfterInsertListener(new EventInfoListener<EntityEditModel.InsertEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.InsertEvent info) {
        handleMasterInsert(info);
      }
    });
    masterModel.getEditModel().addAfterUpdateListener(new EventInfoListener<EntityEditModel.UpdateEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.UpdateEvent info) {
        handleMasterUpdate(info);
      }
    });
    masterModel.getEditModel().addAfterDeleteListener(new EventInfoListener<EntityEditModel.DeleteEvent>() {
      @Override
      public void eventOccurred(final EntityEditModel.DeleteEvent info) {
        handleMasterDelete(info);
      }
    });
  }
}