/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Util;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityModel implementation.
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = Users.user("scott", "tiger");
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

  private final Event<?> refreshStartedEvent = Events.event();
  private final Event<?> refreshDoneEvent = Events.event();
  private final Event<M> linkedDetailModelAddedEvent = Events.event();
  private final Event<M> linkedDetailModelRemovedEvent = Events.event();

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
  private final Map<M, ForeignKeyProperty> detailModelForeignKeys = new HashMap<>();

  /**
   * The master model, if any, so that detail models can refer to their masters
   */
  private M masterModel;

  /**
   * True while the model is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * If true then this models table model will automatically search by the inserted entity
   * when an insert is performed in a master model
   */
  private boolean searchOnMasterInsert = EntityModel.SEARCH_ON_MASTER_INSERT.get();

  /**
   * Instantiates a new DefaultEntityModel
   * @param editModel the edit model
   * @param tableModel the table model
   */
  public DefaultEntityModel(final E editModel, final T tableModel) {
    requireNonNull(editModel, "editModel");
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
    return getClass().getSimpleName() + ": " + getEntityId();
  }

  @Override
  public final Entity.Identity getEntityId() {
    return editModel.getEntityId();
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final M getMasterModel() {
    return masterModel;
  }

  @Override
  public final void setMasterModel(final M entityModel) {
    if (this.masterModel != null) {
      throw new IllegalStateException("Master model has already been set for " + this);
    }
    this.masterModel = entityModel;
  }

  @Override
  public final E getEditModel() {
    return editModel;
  }

  @Override
  public final T getTableModel() {
    return tableModel;
  }

  @Override
  public final boolean containsTableModel() {
    return tableModel != null;
  }

  @Override
  public final void addDetailModels(final M... detailModels) {
    requireNonNull(detailModels, "detailModels");
    for (final M detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

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

  @Override
  public final boolean containsDetailModel(final Class<? extends M> modelClass) {
    return detailModels.stream().anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsDetailModel(final Entity.Identity entityId) {
    return detailModels.stream().anyMatch(detailModel -> detailModel.getEntityId().equals(entityId));
  }

  @Override
  public final boolean containsDetailModel(final M detailModel) {
    return detailModels.contains(detailModel);
  }

  @Override
  public final Collection<M> getDetailModels() {
    return unmodifiableCollection(detailModels);
  }

  @Override
  public final void addLinkedDetailModel(final M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.add(detailModel)) {
      linkedDetailModelAddedEvent.onEvent(detailModel);
    }
  }

  @Override
  public final void removeLinkedDetailModel(final M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.remove(detailModel)) {
      linkedDetailModelRemovedEvent.onEvent(detailModel);
    }
  }

  @Override
  public final Collection<M> getLinkedDetailModels() {
    return unmodifiableCollection(linkedDetailModels);
  }

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

  @Override
  public final M getDetailModel(final Entity.Identity entityId) {
    for (final M detailModel : detailModels) {
      if (detailModel.getEntityId().equals(entityId)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityId + " found in model: " + this);
  }

  @Override
  public final void setDetailModelForeignKey(final M detailModel, final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(detailModel, "detailModel");
    if (!containsDetailModel(detailModel)) {
      throw new IllegalArgumentException(this + " does not contain detail model: " + detailModel);
    }

    if (foreignKeyAttribute == null) {
      detailModelForeignKeys.remove(detailModel);
    }
    else {
      detailModelForeignKeys.put(detailModel,
              connectionProvider.getEntities().getDefinition(detailModel.getEntityId()).getForeignKeyProperty(foreignKeyAttribute));
    }
  }

  @Override
  public final ForeignKeyProperty getDetailModelForeignKey(final M detailModel) {
    return detailModelForeignKeys.get(detailModel);
  }

  @Override
  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      LOG.debug("{} refreshing", this);
      isRefreshing = true;
      refreshStartedEvent.onEvent();
      if (containsTableModel()) {
        tableModel.refresh();
      }
      initializeDetailModels();
    }
    finally {
      isRefreshing = false;
      refreshDoneEvent.onEvent();
      LOG.debug("{} done refreshing", this);
    }
  }

  @Override
  public final void refreshDetailModels() {
    for (final M detailModel : detailModels) {
      detailModel.refresh();
    }
  }

  @Override
  public final void clear() {
    if (containsTableModel()) {
      tableModel.clear();
    }
    editModel.clear();
    clearDetailModels();
  }

  @Override
  public final void clearDetailModels() {
    for (final M detailModel : detailModels) {
      detailModel.clear();
    }
  }

  @Override
  public final void initialize(final Entity.Identity foreignKeyEntityId, final List<Entity> foreignKeyValues) {
    final List<ForeignKeyProperty> foreignKeyProperties =
            editModel.getEntityDefinition().getForeignKeyReferences(foreignKeyEntityId);
    if (!foreignKeyProperties.isEmpty()) {
      initialize(foreignKeyProperties.get(0), foreignKeyValues);
    }
  }

  @Override
  public final void initialize(final ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (containsTableModel()) {
      tableModel.setForeignKeyConditionValues(foreignKeyProperty, foreignKeyValues);
    }
    onInitialization(foreignKeyProperty, foreignKeyValues);
  }

  @Override
  public void savePreferences() {
    if (containsTableModel()) {
      getTableModel().savePreferences();
    }
    getDetailModels().forEach(EntityModel::savePreferences);
  }

  @Override
  public final boolean isSearchOnMasterInsert() {
    return searchOnMasterInsert;
  }

  @Override
  public final void setSearchOnMasterInsert(final boolean searchOnMasterInsert) {
    this.searchOnMasterInsert = searchOnMasterInsert;
  }

  @Override
  public final void addLinkedDetailModelAddedListener(final EventDataListener<M> listener) {
    linkedDetailModelAddedEvent.addDataListener(listener);
  }

  @Override
  public final void removeLinkedDetailModelAddedListener(final EventDataListener<M> listener) {
    linkedDetailModelAddedEvent.removeDataListener(listener);
  }

  @Override
  public final void addLinkedDetailModelRemovedListener(final EventDataListener<M> listener) {
    linkedDetailModelRemovedEvent.addDataListener(listener);
  }

  @Override
  public final void removeLinkedDetailModelRemovedListener(final EventDataListener<M> listener) {
    linkedDetailModelRemovedEvent.removeDataListener(listener);
  }

  @Override
  public final void addBeforeRefreshListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  @Override
  public final void removeBeforeRefreshListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  /**
   * By default this method sets the foreign key value in the edit model if the entity is new, using the first item in {@code foreignKeyValues}.
   * @param foreignKeyProperty the foreign key referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model
   */
  protected void onInitialization(final ForeignKeyProperty foreignKeyProperty, final List<Entity> foreignKeyValues) {
    if (editModel.isEntityNew() && !Util.nullOrEmpty(foreignKeyValues)) {
      editModel.put(foreignKeyProperty.getAttribute(), foreignKeyValues.get(0));
    }
  }

  /**
   * Initializes all linked detail models according to the active entities in this master model
   * @see #getActiveEntities()
   * @see #addLinkedDetailModel(EntityModel)
   * @see #initialize(Entity.Identity, List)
   */
  protected final void initializeDetailModels() {
    final List<Entity> activeEntities = getActiveEntities();
    for (final M detailModel : linkedDetailModels) {
      if (detailModelForeignKeys.containsKey(detailModel)) {
        detailModel.initialize(detailModelForeignKeys.get(detailModel), activeEntities);
      }
      else {
        detailModel.initialize(getEntityId(), activeEntities);
      }
    }
  }

  /**
   * Adds the inserted entities to the EntityComboBoxModels based on the inserted entity type,
   * sets the value of the master foreign key property and filters the table model if applicable
   * @param insertedEntities the inserted entities
   * @see EntityModel#SEARCH_ON_MASTER_INSERT
   */
  protected final void onMasterInsert(final List<Entity> insertedEntities) {
    editModel.addForeignKeyValues(insertedEntities);
    editModel.setForeignKeyValues(insertedEntities);
    if (containsTableModel() && searchOnMasterInsert) {
      ForeignKeyProperty foreignKeyProperty = masterModel.getDetailModelForeignKey((M) this);
      if (foreignKeyProperty == null) {
        foreignKeyProperty = editModel.getEntityDefinition().getForeignKeyReferences(masterModel.getEntityId()).get(0);
      }
      tableModel.setForeignKeyConditionValues(foreignKeyProperty, insertedEntities);
    }
  }

  /**
   * Replaces the updated master entities wherever they are referenced
   * @param updatedEntities the updated entities
   */
  protected final void onMasterUpdate(final Map<Entity.Key, Entity> updatedEntities) {
    editModel.replaceForeignKeyValues(updatedEntities.values());
    if (containsTableModel()) {
      tableModel.replaceForeignKeyValues(masterModel.getEntityId(), updatedEntities.values());
    }
  }

  protected final void onMasterDelete(final List<Entity> deletedEntities) {
    editModel.removeForeignKeyValues(deletedEntities);
  }

  private List<Entity> getActiveEntities() {
    if (tableModel != null && tableModel.getSelectionModel().isSelectionNotEmpty()) {
      return tableModel.getSelectionModel().getSelectedItems();
    }
    else if (editModel.isEntityNew()) {
      return emptyList();
    }

    return singletonList(editModel.getEntityCopy());
  }

  private void setTableEditModel(final E editModel, final EntityTableModel<E> tableModel) {
    if (tableModel != null && !editModel.getEntityId().equals(tableModel.getEntityId())) {
      throw new IllegalArgumentException("Table model entityId mismatch, found: " + tableModel.getEntityId() + ", required: " + editModel.getEntityId());
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
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    if (containsTableModel()) {
      getTableModel().addSelectionChangedListener(initializer);
    }
    else {
      editModel.addEntitySetListener(entity -> initializeDetailModels());
    }
  }

  private void onInsert(final List<Entity> insertedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterInsert(insertedEntities));
  }

  private void onUpdate(final Map<Entity.Key, Entity> updatedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterUpdate(updatedEntities));
  }

  private void onDelete(final List<Entity> deletedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterDelete(deletedEntities));
  }
}