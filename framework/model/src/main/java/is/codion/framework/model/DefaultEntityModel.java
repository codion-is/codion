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
  private final Map<M, EntityModelLink<M, E, T>> detailModels = new HashMap<>();

  /**
   * Holds linked detail models that should be updated and filtered according to the selected entity/entities
   */
  private final Set<M> linkedDetailModels = new HashSet<>();

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
  public final ForeignKeyEntityModelLink<M, E, T> addDetailModel(M detailModel) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    List<ForeignKey> foreignKeys = detailModel.editModel().entityDefinition().foreignKeys(editModel.entityType());
    if (foreignKeys.isEmpty()) {
      throw new IllegalArgumentException("Entity " + detailModel.editModel().entityType() +
              " does not reference " + editModel.entityType() + " via a foreign key");
    }

    return addDetailModel(detailModel, foreignKeys.get(0));
  }

  @Override
  public final ForeignKeyEntityModelLink<M, E, T> addDetailModel(M detailModel, ForeignKey foreignKey) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    requireNonNull(foreignKey, "foreignKey");
    if (this == detailModel) {
      throw new IllegalArgumentException("A model can not be its own detail model");
    }
    if (detailModels.containsKey(detailModel)) {
      throw new IllegalArgumentException("Detail model " + detailModel + " has already been added");
    }

    return addDetailModel(new DefaultForeignKeyEntityModelLink<>(detailModel, foreignKey));
  }

  @Override
  public final <L extends EntityModelLink<M, E, T>> L addDetailModel(L modelLink) {
    requireNonNull(modelLink, "modelLink");
    detailModels.put(modelLink.detailModel(), modelLink);

    return modelLink;
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
  public final <L extends EntityModelLink<M, E, T>> L detailModelLink(M detailModel) {
    if (!detailModels.containsKey(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }

    return (L) detailModels.get(detailModel);
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
  public void savePreferences() {
    if (containsTableModel()) {
      tableModel().savePreferences();
    }
    detailModels().forEach(EntityModel::savePreferences);
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

  private void onMasterSelectionChanged() {
    List<Entity> activeEntities = activeEntities();
    for (M detailModel : linkedDetailModels) {
      detailModels.get(detailModel).onSelection(activeEntities);
    }
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
    EventListener onMasterSelectionChanged = this::onMasterSelectionChanged;
    linkedDetailModelAddedEvent.addListener(onMasterSelectionChanged);
    linkedDetailModelRemovedEvent.addListener(onMasterSelectionChanged);
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    if (containsTableModel()) {
      tableModel.addSelectionListener(onMasterSelectionChanged);
    }
    else {
      editModel.addEntityListener(entity -> onMasterSelectionChanged());
    }
  }

  private void onInsert(List<Entity> insertedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onInsert(insertedEntities));
  }

  private void onUpdate(Map<Key, Entity> updatedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onUpdate(updatedEntities));
  }

  private void onDelete(List<Entity> deletedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onDelete(deletedEntities));
  }
}