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
  private final Map<M, ForeignKey> detailModels = new HashMap<>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final Set<M> linkedDetailModels = new HashSet<>();

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
    List<ForeignKey> foreignKeys = detailModel.editModel().entityDefinition().foreignKeys(editModel.entityType());
    if (foreignKeys.isEmpty()) {
      throw new IllegalArgumentException("Entity " + detailModel.editModel().entityType() +
              " does not reference " + editModel.entityType() + " via a foreign key");
    }

    return addDetailModel(detailModel, foreignKeys.get(0));
  }

  @Override
  public final M addDetailModel(M detailModel, ForeignKey foreignKey) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    requireNonNull(foreignKey, "foreignKey");
    if (this == detailModel) {
      throw new IllegalArgumentException("A model can not be its own detail model");
    }
    if (detailModels.containsKey(detailModel)) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already been added");
    }
    if (detailModel.getMasterModel() != null) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already had a master model defined");
    }
    detailModels.put(detailModel, foreignKey);
    detailModel.setMasterModel((M) this);
    if (detailModel.containsTableModel()) {
      detailModel.tableModel().queryConditionRequiredState().set(true);
    }

    return detailModel;
  }

  @Override
  public final boolean containsDetailModel(Class<? extends M> modelClass) {
    requireNonNull(modelClass, "modelClass");
    return detailModels.keySet().stream()
            .anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsDetailModel(EntityType entityType) {
    requireNonNull(entityType, "entityType");
    return detailModels.keySet().stream()
            .anyMatch(detailModel -> detailModel.entityType().equals(entityType));
  }

  @Override
  public final boolean containsDetailModel(M detailModel) {
    return detailModels.containsKey(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
  }

  @Override
  public final Collection<M> detailModels() {
    return unmodifiableCollection(detailModels.keySet());
  }

  @Override
  public final void addLinkedDetailModel(M detailModel) {
    if (!detailModels.containsKey(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }
    if (linkedDetailModels.add(detailModel)) {
      linkedDetailModelAddedEvent.onEvent(detailModel);
    }
  }

  @Override
  public final void removeLinkedDetailModel(M detailModel) {
    if (!detailModels.containsKey(requireNonNull(detailModel))) {
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
    for (M detailModel : detailModels.keySet()) {
      if (detailModel.getClass().equals(modelClass)) {
        return (T) detailModel;
      }
    }

    throw new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found");
  }

  @Override
  public final M detailModel(EntityType entityType) {
    requireNonNull(entityType, "entityType");
    for (M detailModel : detailModels.keySet()) {
      if (detailModel.entityType().equals(entityType)) {
        return detailModel;
      }
    }

    throw new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this);
  }

  @Override
  public final ForeignKey detailModelForeignKey(M detailModel) {
    return detailModels.get(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
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
    for (M detailModel : detailModels.keySet()) {
      detailModel.clear();
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
   * @see #initialize(ForeignKey, List)
   */
  protected final void initializeDetailModels() {
    List<Entity> activeEntities = activeEntities();
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
    detailModel.initialize(detailModels.get(detailModel), activeEntities);
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
      tableModel.setForeignKeyConditionValues(masterModel.detailModelForeignKey((M) this), insertedEntities);
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

  private List<Entity> activeEntities() {
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
      editModel.addEntityListener(entity -> initializeDetailModels());
    }
  }

  private void onInsert(List<Entity> insertedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModel.onMasterInsert(insertedEntities));
  }

  private void onUpdate(Map<Key, Entity> updatedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModel.onMasterUpdate(updatedEntities));
  }

  private void onDelete(List<Entity> deletedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModel.onMasterDelete(deletedEntities));
  }
}