/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityModel implementation.
 * @param <M> the type of {@link DefaultEntityModel} used for detail models
 * @param <E> the type of {@link AbstractEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityModel<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
        T extends EntityTableModel<E>> implements EntityModel<M, E, T> {

  private static final String DETAIL_MODEL_PARAMETER = "detailModel";

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
  private final Map<M, DetailModelLink<M, E, T>> detailModels = new HashMap<>();
  private final Event<Collection<M>> activeDetailModelsEvent = Event.event();

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
  public final <C extends E> C editModel() {
    return (C) editModel;
  }

  @Override
  public final <C extends T> C tableModel() {
    if (tableModel == null) {
      throw new IllegalStateException("Entity model " + this + " does not contain a table model");
    }

    return (C) tableModel;
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
  public final ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    Collection<ForeignKey> foreignKeys = detailModel.editModel().entityDefinition().foreignKeys(editModel.entityType());
    if (foreignKeys.isEmpty()) {
      throw new IllegalArgumentException("Entity " + detailModel.editModel().entityType() +
              " does not reference " + editModel.entityType() + " via a foreign key");
    }

    return addDetailModel(detailModel, foreignKeys.iterator().next());
  }

  @Override
  public final ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel, ForeignKey foreignKey) {
    requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
    requireNonNull(foreignKey, "foreignKey");

    return addDetailModel(new DefaultForeignKeyDetailModelLink<>(detailModel, foreignKey));
  }

  @Override
  public final <L extends DetailModelLink<M, E, T>> L addDetailModel(L detailModelLink) {
    requireNonNull(detailModelLink, "detailModelLink");
    if (this == detailModelLink.detailModel()) {
      throw new IllegalArgumentException("A model can not be its own detail model");
    }
    if (detailModels.containsKey(detailModelLink.detailModel())) {
      throw new IllegalArgumentException("Detail model " + detailModelLink.detailModel() + " has already been added");
    }
    detailModels.put(detailModelLink.detailModel(), detailModelLink);
    detailModelLink.activeObserver().addListener(() -> activeDetailModelChanged(detailModelLink));

    return detailModelLink;
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
  public final <L extends DetailModelLink<M, E, T>> L detailModelLink(M detailModel) {
    if (!detailModels.containsKey(requireNonNull(detailModel))) {
      throw new IllegalStateException("Detail model not found: " + detailModel);
    }

    return (L) detailModels.get(detailModel);
  }

  @Override
  public final Collection<M> activeDetailModels() {
    return detailModels.values().stream()
            .filter(DetailModelLink::isActive)
            .map(DetailModelLink::detailModel)
            .collect(Collectors.toList());
  }

  @Override
  public final void addActiveDetailModelsListener(Consumer<Collection<M>> listener) {
    activeDetailModelsEvent.addDataListener(listener);
  }

  @Override
  public final void removeActiveDetailModelsListener(Consumer<Collection<M>> listener) {
    activeDetailModelsEvent.removeDataListener(listener);
  }

  @Override
  public final <C extends M> C detailModel(Class<C> modelClass) {
    requireNonNull(modelClass, "modelClass");
    return (C) detailModels.keySet().stream()
            .filter(detailModel -> detailModel.getClass().equals(modelClass))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found in model: " + this));
  }

  @Override
  public final <C extends M> C detailModel(EntityType entityType) {
    requireNonNull(entityType, "entityType");
    return (C) detailModels.keySet().stream()
            .filter(detailModel -> detailModel.entityType().equals(entityType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this));
  }

  @Override
  public void savePreferences() {
    if (containsTableModel()) {
      tableModel().savePreferences();
    }
    detailModels().forEach(EntityModel::savePreferences);
  }

  private void activeDetailModelChanged(DetailModelLink<M, E, T> detailModelLink) {
    activeDetailModelsEvent.accept(activeDetailModels());
    detailModelLink.onSelection(activeEntities());
  }

  private void onMasterSelectionChanged() {
    List<Entity> activeEntities = activeEntities();
    for (M detailModel : activeDetailModels()) {
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

    return singletonList(editModel.entity());
  }

  private void bindEventsInternal() {
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    if (containsTableModel()) {
      tableModel.addSelectionListener(this::onMasterSelectionChanged);
    }
    else {
      editModel.addEntityListener(entity -> onMasterSelectionChanged());
    }
  }

  private void onInsert(Collection<Entity> insertedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onInsert(insertedEntities));
  }

  private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onUpdate(updatedEntities));
  }

  private void onDelete(Collection<Entity> deletedEntities) {
    detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onDelete(deletedEntities));
  }
}