/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityModel implementation.
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeId);
 *
 * EntityModel model = new DefaultEntityModel(entityId, connectionProvider);
 *
 * EntityPanel panel = new EntityPanel(model);
 * </pre>
 * @param <M> the type of {@link DefaultEntityModel} used for detail models
 * @param <E> the type of {@link DefaultEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityModel<M extends DefaultEntityModel<M, E, T>, E extends DefaultEntityEditModel,
        T extends EntityTableModel<E>> implements EntityModel<M, E, T> {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModel.class);

  private final Event refreshStartedEvent = Events.event();
  private final Event refreshDoneEvent = Events.event();
  private final Event<M> linkedDetailModelAddedEvent = Events.event();
  private final Event<M> linkedDetailModelRemovedEvent = Events.event();

  /**
   * The entity ID
   */
  private final String entityId;

  /**
   * The EntityEditModel instance
   */
  private final E editModel;

  /**
   * The EntityTableModel model
   */
  private final T tableModel;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Holds the detail EntityModels used by this EntityModel
   */
  private final Collection<M> detailModels = new ArrayList<>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final Set<M> linkedDetailModels = new HashSet<>();

  /**
   * Maps detail models to the foreign key property they are based on
   */
  private final Map<M, Property.ForeignKeyProperty> detailModelForeignKeys = new HashMap<>();

  /**
   * The master model, if any, so that detail models can refer to their masters
   */
  private M masterModel;

  /**
   * True while the model is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * If true then the table model is automatically filtered when insert is performed in a master model
   */
  private boolean filterOnMasterInsert = EntityModel.FILTER_ON_MASTER_INSERT.get();

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public DefaultEntityModel(final E editModel, final T tableModel) {
    requireNonNull(editModel, "editModel");
    this.entityId = editModel.getEntityId();
    this.connectionProvider = editModel.getConnectionProvider();
    this.editModel = editModel;
    this.tableModel = tableModel;
    setTableEditModel(editModel, tableModel);
    bindEventsInternal();
    editModel.setEntity(null);
  }

  /**
   * @return a String representation of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final M getMasterModel() {
    return masterModel;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMasterModel(final M entityModel) {
    if (this.masterModel != null) {
      throw new IllegalStateException("Master model has already been set for " + this);
    }
    this.masterModel = entityModel;
  }

  /** {@inheritDoc} */
  @Override
  public final E getEditModel() {
    return editModel;
  }

  /** {@inheritDoc} */
  @Override
  public final T getTableModel() {
    return tableModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsTableModel() {
    return tableModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final void addDetailModels(final M... detailModels) {
    requireNonNull(detailModels, "detailModels");
    for (final M detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final M addDetailModel(final M detailModel) {
    if (this.detailModels.contains(detailModel)) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already been added");
    }
    if (detailModel.getMasterModel() != null) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already had a master model defined");
    }
    this.detailModels.add(detailModel);
    detailModel.setMasterModel((M) this);
    if (detailModel.containsTableModel()) {
      detailModel.getTableModel().getQueryConditionRequiredState().set(true);
    }

    return detailModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final Class<? extends M> modelClass) {
    return detailModels.stream().anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final String entityId) {
    return detailModels.stream().anyMatch(detailModel -> detailModel.getEntityId().equals(entityId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsDetailModel(final M detailModel) {
    return detailModels.contains(detailModel);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<M> getDetailModels() {
    return Collections.unmodifiableCollection(detailModels);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLinkedDetailModel(final M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.add(detailModel)) {
      linkedDetailModelAddedEvent.fire(detailModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLinkedDetailModel(final M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.remove(detailModel)) {
      linkedDetailModelRemovedEvent.fire(detailModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<M> getLinkedDetailModels() {
    return Collections.unmodifiableCollection(linkedDetailModels);
  }

  /** {@inheritDoc} */
  @Override
  public final M getDetailModel(final Class<? extends M> modelClass) {
    requireNonNull(modelClass, "modelClass");
    for (final M detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found");
  }

  /** {@inheritDoc} */
  @Override
  public final M getDetailModel(final String entityId) {
    for (final M detailModel : detailModels) {
      if (detailModel.getEntityId().equals(entityId)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityId + " found in model: " + this);
  }

  /** {@inheritDoc} */
  @Override
  public final void setDetailModelForeignKey(final M detailModel, final String foreignKeyPropertyId) {
    requireNonNull(detailModel, "detailModel");
    if (!containsDetailModel(detailModel)) {
      throw new IllegalArgumentException(this + " does not contain detail model: " + detailModel);
    }

    if (foreignKeyPropertyId == null) {
      detailModelForeignKeys.remove(detailModel);
    }
    else {
      detailModelForeignKeys.put(detailModel,
              connectionProvider.getDomain().getDefinition(detailModel.getEntityId()).getForeignKeyProperty(foreignKeyPropertyId));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Property.ForeignKeyProperty getDetailModelForeignKey(final M detailModel) {
    return detailModelForeignKeys.get(detailModel);
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
    for (final M detailModel : detailModels) {
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
    for (final M detailModel : detailModels) {
      detailModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void initialize(final String foreignKeyEntityId, final List<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = connectionProvider.getDomain()
            .getDefinition(entityId).getForeignKeyProperties(foreignKeyEntityId);
    if (!foreignKeyProperties.isEmpty()) {
      initialize(foreignKeyProperties.get(0), foreignKeyValues);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void initialize(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (containsTableModel()) {
      tableModel.setForeignKeyConditionValues(foreignKeyProperty, foreignKeyValues);
    }
    handleInitialization(foreignKeyProperty, foreignKeyValues);
  }

  /** {@inheritDoc} */
  @Override
  public void savePreferences() {
    if (containsTableModel()) {
      getTableModel().savePreferences();
    }
    getDetailModels().forEach(EntityModel::savePreferences);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFilterOnMasterInsert() {
    return filterOnMasterInsert;
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterOnMasterInsert(final boolean filterOnMasterInsert) {
    this.filterOnMasterInsert = filterOnMasterInsert;
  }

  /** {@inheritDoc} */
  @Override
  public final void addLinkedDetailModelAddedListener(final EventDataListener<M> listener) {
    linkedDetailModelAddedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLinkedDetailModelAddedListener(final EventDataListener listener) {
    linkedDetailModelAddedEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLinkedDetailModelRemovedListener(final EventDataListener<M> listener) {
    linkedDetailModelRemovedEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLinkedDetailModelRemovedListener(final EventDataListener listener) {
    linkedDetailModelRemovedEvent.removeDataListener(listener);
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
   * By default this method sets the foreign key value in the edit model if the entity is new, using the first item in {@code foreignKeyValues}.
   * @param foreignKeyProperty the foreign key referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model
   */
  protected void handleInitialization(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (editModel.isEntityNew() && !Util.nullOrEmpty(foreignKeyValues)) {
      editModel.put(foreignKeyProperty, foreignKeyValues.get(0));
    }
  }

  /**
   * Initializes all linked detail models according to the active entities in this master model
   * @see #getActiveEntities()
   * @see #addLinkedDetailModel(EntityModel)
   * @see #initialize(org.jminor.framework.domain.Property.ForeignKeyProperty, java.util.List)
   * @see #initialize(String, java.util.List)
   */
  protected final void initializeDetailModels() {
    final List<Entity> activeEntities = getActiveEntities();
    for (final M detailModel : linkedDetailModels) {
      if (detailModelForeignKeys.containsKey(detailModel)) {
        detailModel.initialize(detailModelForeignKeys.get(detailModel), activeEntities);
      }
      else {
        detailModel.initialize(entityId, activeEntities);
      }
    }
  }

  /**
   * Adds the inserted entities to the EntityComboBoxModels based on the inserted entity type,
   * sets the value of the master foreign key property and filters the table model if applicable
   * @param insertEvent the insert event
   * @see EntityModel#FILTER_ON_MASTER_INSERT
   */
  protected final void handleMasterInsert(final EntityEditModel.InsertEvent insertEvent) {
    editModel.addForeignKeyValues(insertEvent.getInsertedEntities());
    editModel.setForeignKeyValues(insertEvent.getInsertedEntities());
    if (containsTableModel() && filterOnMasterInsert) {
      Property.ForeignKeyProperty foreignKeyProperty = masterModel.getDetailModelForeignKey((M) this);
      if (foreignKeyProperty == null) {
        foreignKeyProperty = connectionProvider.getDomain()
                .getDefinition(entityId).getForeignKeyProperties(masterModel.getEntityId()).get(0);
      }
      tableModel.setForeignKeyConditionValues(foreignKeyProperty, insertEvent.getInsertedEntities());
    }
  }

  /**
   * Replaces the updated master entities wherever they are referenced
   * @param updateEvent the update event
   */
  protected final void handleMasterUpdate(final EntityEditModel.UpdateEvent updateEvent) {
    editModel.replaceForeignKeyValues(masterModel.getEntityId(), updateEvent.getUpdatedEntities().values());
    if (containsTableModel()) {
      tableModel.replaceForeignKeyValues(masterModel.getEntityId(), updateEvent.getUpdatedEntities().values());
    }
  }

  protected final void handleMasterDelete(final EntityEditModel.DeleteEvent deleteEvent) {
    editModel.removeForeignKeyValues(deleteEvent.getDeletedEntities());
  }

  private List<Entity> getActiveEntities() {
    final List<Entity> activeEntities;
    if (containsTableModel() && !tableModel.getSelectionModel().getSelectionEmptyObserver().get()) {
      activeEntities = tableModel.getSelectionModel().getSelectedItems();
    }
    else {
      if (editModel.isEntityNew()) {
        activeEntities = emptyList();
      }
      else {
        activeEntities = singletonList(editModel.getEntityCopy());
      }
    }
    return activeEntities;
  }

  private void setTableEditModel(final EntityEditModel editModel, final EntityTableModel tableModel) {
    if (tableModel != null && !entityId.equals(tableModel.getEntityId())) {
      throw new IllegalArgumentException("Table model entityId mismatch, found: " + tableModel.getEntityId() + ", required: " + entityId);
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

  private void bindEventsInternal() {
    final EventListener initializer = this::initializeDetailModels;
    linkedDetailModelAddedEvent.addListener(initializer);
    linkedDetailModelRemovedEvent.addListener(initializer);
    editModel.addAfterInsertListener(insertEvent ->
            detailModels.forEach(detailModel -> detailModel.handleMasterInsert(insertEvent)));
    editModel.addAfterUpdateListener(updateEvent ->
            detailModels.forEach(detailModel -> detailModel.handleMasterUpdate(updateEvent)));
    editModel.addAfterDeleteListener(deleteEvent ->
            detailModels.forEach(detailModel -> detailModel.handleMasterDelete(deleteEvent)));
    if (containsTableModel()) {
      getTableModel().addSelectionChangedListener(initializer);
    }
    else {
      editModel.addEntitySetListener(entity -> initializeDetailModels());
    }
  }
}