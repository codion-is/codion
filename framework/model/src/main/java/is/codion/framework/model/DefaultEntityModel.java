/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  public DefaultEntityModel(E editModel) {
    requireNonNull(editModel, "editModel");
    this.connectionProvider = editModel.connectionProvider();
    this.editModel = editModel;
    this.tableModel = null;
    bindEventsInternal();
  }

  /**
   * Instantiates a new DefaultEntityModel
   * @param tableModel the table model
   */
  public DefaultEntityModel(T tableModel) {
    requireNonNull(tableModel, "tableModel");
    this.connectionProvider = tableModel.connectionProvider();
    this.editModel = tableModel.editModel();
    this.tableModel = tableModel;
    bindEventsInternal();
  }

  /**
   * @return a String representation of this EntityModel,
   * returns the model class name by default
   */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityType();
  }

  @Override
  public final EntityType entityType() {
    return editModel.entityType();
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final Entities entities() {
    return connectionProvider.entities();
  }

  @Override
  public final M getMasterModel() {
    return masterModel;
  }

  @Override
  public final void setMasterModel(M entityModel) {
    requireNonNull(entityModel, "entityModel");
    if (this.masterModel != null) {
      throw new IllegalStateException("Master model has already been set for " + this);
    }
    this.masterModel = entityModel;
  }

  @Override
  public final E editModel() {
    return editModel;
  }

  @Override
  public final T tableModel() {
    return tableModel;
  }

  @Override
  public final boolean containsTableModel() {
    return tableModel != null;
  }

  @Override
  @SafeVarargs
  public final void addDetailModels(M... detailModels) {
    requireNonNull(detailModels, "detailModels");
    for (M detailModel : detailModels) {
      addDetailModel(detailModel);
    }
  }

  @Override
  public final M addDetailModel(M detailModel) {
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
      detailModel.tableModel().queryConditionRequiredState().set(true);
    }

    return detailModel;
  }

  @Override
  public final boolean containsDetailModel(Class<? extends M> modelClass) {
    requireNonNull(modelClass, "modelClass");
    return detailModels.stream()
            .anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsDetailModel(EntityType entityType) {
    requireNonNull(entityType, "entityType");
    return detailModels.stream()
            .anyMatch(detailModel -> detailModel.entityType().equals(entityType));
  }

  @Override
  public final boolean containsDetailModel(M detailModel) {
    return detailModels.contains(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
  }

  @Override
  public final Collection<M> detailModels() {
    return unmodifiableCollection(detailModels);
  }

  @Override
  public final void addLinkedDetailModel(M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.add(detailModel)) {
      linkedDetailModelAddedEvent.onEvent(detailModel);
    }
  }

  @Override
  public final void removeLinkedDetailModel(M detailModel) {
    if (!detailModels.contains(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.remove(detailModel)) {
      linkedDetailModelRemovedEvent.onEvent(detailModel);
    }
  }

  @Override
  public final Collection<M> linkedDetailModels() {
    return unmodifiableCollection(linkedDetailModels);
  }

  @Override
  public final <T extends M> T detailModel(Class<? extends M> modelClass) {
    requireNonNull(modelClass, "modelClass");
    for (M detailModel : detailModels) {
      if (detailModel.getClass().equals(modelClass)) {
        return (T) detailModel;
      }
    }

    throw new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found");
  }

  @Override
  public final M detailModel(EntityType entityType) {
    requireNonNull(entityType, "entityType");
    for (M detailModel : detailModels) {
      if (detailModel.entityType().equals(entityType)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this);
  }

  @Override
  public final void setDetailModelForeignKey(M detailModel, ForeignKey foreignKey) {
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
  public final ForeignKey detailModelForeignKey(M detailModel) {
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
    for (M detailModel : detailModels) {
      detailModel.clear();
    }
  }

  @Override
  public final void initialize(EntityType foreignKeyEntityType, List<Entity> foreignKeyValues) {
    requireNonNull(foreignKeyEntityType);
    requireNonNull(foreignKeyValues);
    List<ForeignKey> foreignKeys = editModel.entityDefinition().foreignKeys(foreignKeyEntityType);
    if (!foreignKeys.isEmpty()) {
      initialize(foreignKeys.get(0), foreignKeyValues);
    }
  }

  @Override
  public final void initialize(ForeignKey foreignKey, List<Entity> foreignKeyValues) {
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
      tableModel().savePreferences();
    }
    detailModels().forEach(EntityModel::savePreferences);
  }

  @Override
  public final boolean isSearchOnMasterInsert() {
    return searchOnMasterInsert;
  }

  @Override
  public final void setSearchOnMasterInsert(boolean searchOnMasterInsert) {
    this.searchOnMasterInsert = searchOnMasterInsert;
  }

  @Override
  public final void addLinkedDetailModelAddedListener(EventDataListener<M> listener) {
    linkedDetailModelAddedEvent.addDataListener(listener);
  }

  @Override
  public final void removeLinkedDetailModelAddedListener(EventDataListener<M> listener) {
    linkedDetailModelAddedEvent.removeDataListener(listener);
  }

  @Override
  public final void addLinkedDetailModelRemovedListener(EventDataListener<M> listener) {
    linkedDetailModelRemovedEvent.addDataListener(listener);
  }

  @Override
  public final void removeLinkedDetailModelRemovedListener(EventDataListener<M> listener) {
    linkedDetailModelRemovedEvent.removeDataListener(listener);
  }

  /**
   * By default, this method initializes the edit model according to the given foreign key values, using the first item in {@code foreignKeyValues}.
   * @param foreignKey the foreign key attribute referring to the master model doing the initialization
   * @param foreignKeyValues the foreign key entities selected or otherwise indicated as being active in the master model, empty list for none
   * @see EntityEditModel#initialize(ForeignKey, Entity)
   */
  protected void onInitialization(ForeignKey foreignKey, List<Entity> foreignKeyValues) {
    editModel.initialize(foreignKey, foreignKeyValues.isEmpty() ? null : foreignKeyValues.get(0));
  }

  /**
   * Initializes all linked detail models according to the active entities in this master model
   * @see #addLinkedDetailModel(DefaultEntityModel)
   * @see #initialize(EntityType, List)
   */
  protected final void initializeDetailModels() {
    List<Entity> activeEntities = getActiveEntities();
    for (M detailModel : linkedDetailModels) {
      initializeDetailModel(activeEntities, detailModel);
    }
  }

  /**
   * Initializes the given detail model according to the given active master entities.
   * @param activeEntities the currently active master entities
   * @param detailModel the detail model
   */
  protected void initializeDetailModel(List<Entity> activeEntities, M detailModel) {
    if (detailModelForeignKeys.containsKey(detailModel)) {
      detailModel.initialize(detailModelForeignKeys.get(detailModel), activeEntities);
    }
    else {
      detailModel.initialize(entityType(), activeEntities);
    }
  }

  /**
   * Adds the inserted entities to the EntityComboBoxModels based on the inserted entity type,
   * sets the value of the master foreign key attribute and filters the table model if applicable
   * @param insertedEntities the inserted entities
   * @see EntityModel#SEARCH_ON_MASTER_INSERT
   */
  protected final void onMasterInsert(List<Entity> insertedEntities) {
    editModel.addForeignKeyValues(insertedEntities);
    editModel.setForeignKeyValues(insertedEntities);
    if (containsTableModel() && searchOnMasterInsert) {
      ForeignKey foreignKey = masterModel.detailModelForeignKey((M) this);
      if (foreignKey == null) {
        foreignKey = editModel.entityDefinition().foreignKeys(masterModel.entityType()).get(0);
      }
      tableModel.setForeignKeyConditionValues(foreignKey, insertedEntities);
    }
  }

  /**
   * Replaces the updated master entities wherever they are referenced
   * @param updatedEntities the updated entities
   */
  protected final void onMasterUpdate(Map<Key, Entity> updatedEntities) {
    editModel.replaceForeignKeyValues(updatedEntities.values());
    if (containsTableModel()) {
      tableModel.replaceForeignKeyValues(masterModel.entityType(), updatedEntities.values());
    }
  }

  protected final void onMasterDelete(List<Entity> deletedEntities) {
    editModel.removeForeignKeyValues(deletedEntities);
  }

  private List<Entity> getActiveEntities() {
    if (tableModel != null && tableModel.selectionModel().isSelectionNotEmpty()) {
      return tableModel.selectionModel().getSelectedItems();
    }
    else if (editModel.isEntityNew()) {
      return emptyList();
    }

    return singletonList(editModel.entityCopy());
  }

  private void bindEventsInternal() {
    EventListener initializer = this::initializeDetailModels;
    linkedDetailModelAddedEvent.addListener(initializer);
    linkedDetailModelRemovedEvent.addListener(initializer);
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    if (containsTableModel()) {
      tableModel().addSelectionChangedListener(initializer);
    }
    else {
      editModel.addEntitySetListener(entity -> initializeDetailModels());
    }
  }

  private void onInsert(List<Entity> insertedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterInsert(insertedEntities));
  }

  private void onUpdate(Map<Key, Entity> updatedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterUpdate(updatedEntities));
  }

  private void onDelete(List<Entity> deletedEntities) {
    detailModels.forEach(detailModel -> detailModel.onMasterDelete(deletedEntities));
  }
}