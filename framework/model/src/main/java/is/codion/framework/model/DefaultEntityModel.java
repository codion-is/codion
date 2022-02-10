/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

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
 * @param <M> the type of {@link DefaultEntityModel} used for detail models
 * @param <E> the type of {@link DefaultEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityModel<M extends DefaultEntityModel<M, E, T>, E extends DefaultEntityEditModel,
        T extends EntityTableModel<E>> implements EntityModel<M, E, T> {

  protected static final Logger LOG = LoggerFactory.getLogger(DefaultEntityModel.class);

  private static final String DETAIL_MODEL_PARAMETER = "detailModel";

  private final Event<M> linkedDetailModelAddedEvent = Event.event();
  private final Event<M> linkedDetailModelRemovedEvent = Event.event();

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
   * Maps detail models to the foreign key attribute they are based on
   */
  private final Map<M, ForeignKey> detailModelForeignKeys = new HashMap<>();

  /**
   * The master model, if any, so that detail models can refer to their masters
   */
  private M masterModel;

  /**
   * If true then this models table model will automatically search by the inserted entity
   * when an insert is performed in a master model
   */
  private boolean searchOnMasterInsert = EntityModel.SEARCH_ON_MASTER_INSERT.get();

  /**
   * Instantiates a new DefaultEntityModel, without a table model
   * @param editModel the edit model
   */
  public DefaultEntityModel(final E editModel) {
    requireNonNull(editModel, "editModel");
    this.connectionProvider = editModel.getConnectionProvider();
    this.editModel = editModel;
    this.tableModel = null;
    bindEventsInternal();
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param tableModel the table model
   */
  public DefaultEntityModel(final T tableModel) {
    requireNonNull(tableModel, "tableModel");
    this.connectionProvider = tableModel.getConnectionProvider();
    this.editModel = tableModel.getEditModel();
    this.tableModel = tableModel;
    bindEventsInternal();
  }

  /**
   * @return a String representation of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + getEntityType();
  }

  @Override
  public final EntityType getEntityType() {
    return editModel.getEntityType();
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final Entities getEntities() {
    return connectionProvider.getEntities();
  }

  @Override
  public final M getMasterModel() {
    return masterModel;
  }

  @Override
  public final void setMasterModel(final M entityModel) {
    requireNonNull(entityModel, "entityModel");
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
  @SafeVarargs
  public final void addDetailModels(final M... detailModels) {
    requireNonNull(detailModels, "detailModels");
    for (final M detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  @Override
  public final M addDetailModel(final M detailModel) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
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
    requireNonNull(modelClass, "modelClass");
    return detailModels.stream()
            .anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsDetailModel(final EntityType entityType) {
    requireNonNull(entityType, "entityType");
    return detailModels.stream()
            .anyMatch(detailModel -> detailModel.getEntityType().equals(entityType));
  }

  @Override
  public final boolean containsDetailModel(final M detailModel) {
    return detailModels.contains(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
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
  public final <T extends M> T getDetailModel(final Class<? extends M> modelClass) {
    requireNonNull(modelClass, "modelClass");
    for (final M detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return (T) detailModel;
      }
    }

    throw new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found");
  }

  @Override
  public final M getDetailModel(final EntityType entityType) {
    requireNonNull(entityType, "entityType");
    for (final M detailModel : detailModels) {
      if (detailModel.getEntityType().equals(entityType)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this);
  }

  @Override
  public final void setDetailModelForeignKey(final M detailModel, final ForeignKey foreignKey) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    if (!containsDetailModel(detailModel)) {
      throw new IllegalArgumentException(this + " does not contain detail model: " + detailModel);
    }

    if (foreignKey == null) {
      detailModelForeignKeys.remove(detailModel);
    }
    else {
      detailModelForeignKeys.put(detailModel, foreignKey);
    }
  }

  @Override
  public final ForeignKey getDetailModelForeignKey(final M detailModel) {
    return detailModelForeignKeys.get(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
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
  public final void initialize(final EntityType foreignKeyEntityType, final List<Entity> foreignKeyValues) {
    requireNonNull(foreignKeyEntityType);
    requireNonNull(foreignKeyValues);
    final List<ForeignKey> foreignKeys = editModel.getEntityDefinition().getForeignKeys(foreignKeyEntityType);
    if (!foreignKeys.isEmpty()) {
      initialize(foreignKeys.get(0), foreignKeyValues);
    }
  }

  @Override
  public final void initialize(final ForeignKey foreignKey, final List<Entity> foreignKeyValues) {
    requireNonNull(foreignKey);
    requireNonNull(foreignKeyValues);
    if (containsTableModel()) {
      tableModel.setForeignKeyConditionValues(foreignKey, foreignKeyValues);
    }
    onInitialization(foreignKey, foreignKeyValues);
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

  /**
   * By default, this method initializes the edit model according to the given foreign key values, using the first item in {@code foreignKeyValues}.
   * @param foreignKey the foreign key attribute referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model, empty list for none
   * @see EntityEditModel#initialize(ForeignKey, Entity)
   */
  protected void onInitialization(final ForeignKey foreignKey, final List<Entity> foreignKeyValues) {
    editModel.initialize(foreignKey, foreignKeyValues.isEmpty() ? null : foreignKeyValues.get(0));
  }

  /**
   * Initializes all linked detail models according to the active entities in this master model
   * @see #addLinkedDetailModel(DefaultEntityModel)
   * @see #initialize(EntityType, List)
   */
  protected final void initializeDetailModels() {
    final List<Entity> activeEntities = getActiveEntities();
    for (final M detailModel : linkedDetailModels) {
      initializeDetailModel(activeEntities, detailModel);
    }
  }

  /**
   * Initializes the given detail model according to the given active master entities.
   * @param activeEntities the currently active master entities
   * @param detailModel the detail model
   */
  protected void initializeDetailModel(final List<Entity> activeEntities, final M detailModel) {
    if (detailModelForeignKeys.containsKey(detailModel)) {
      detailModel.initialize(detailModelForeignKeys.get(detailModel), activeEntities);
    }
    else {
      detailModel.initialize(getEntityType(), activeEntities);
    }
  }

  /**
   * Adds the inserted entities to the EntityComboBoxModels based on the inserted entity type,
   * sets the value of the master foreign key attribute and filters the table model if applicable
   * @param insertedEntities the inserted entities
   * @see EntityModel#SEARCH_ON_MASTER_INSERT
   */
  protected final void onMasterInsert(final List<Entity> insertedEntities) {
    editModel.addForeignKeyValues(insertedEntities);
    editModel.setForeignKeyValues(insertedEntities);
    if (containsTableModel() && searchOnMasterInsert) {
      ForeignKey foreignKey = masterModel.getDetailModelForeignKey((M) this);
      if (foreignKey == null) {
        foreignKey = editModel.getEntityDefinition().getForeignKeys(masterModel.getEntityType()).get(0);
      }
      tableModel.setForeignKeyConditionValues(foreignKey, insertedEntities);
    }
  }

  /**
   * Replaces the updated master entities wherever they are referenced
   * @param updatedEntities the updated entities
   */
  protected final void onMasterUpdate(final Map<Key, Entity> updatedEntities) {
    editModel.replaceForeignKeyValues(updatedEntities.values());
    if (containsTableModel()) {
      tableModel.replaceForeignKeyValues(masterModel.getEntityType(), updatedEntities.values());
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

  private void onUpdate(final Map<Key, Entity> updatedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterUpdate(updatedEntities));
  }

  private void onDelete(final List<Entity> deletedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterDelete(deletedEntities));
  }
}