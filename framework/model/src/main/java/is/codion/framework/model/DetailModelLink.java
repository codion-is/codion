/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import java.util.List;
import java.util.Map;

/**
 * Represents a link between a master and detail model.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public interface DetailModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * @return the detail model
   */
  M detailModel();

  /**
   * @return an observer for the active state
   */
  StateObserver activeObserver();

  /**
   * @return true if this link is active
   */
  boolean isActive();

  /**
   * Sets the active state of this link. Active detail model handlers update and filter
   * the detail model according to the entity/entities selected in this (the master) model.
   * @param active true if this link should be activated
   */
  void setActive(boolean active);

  /**
   * Called when the selection changes in the master model
   * @param selectedEntities the selected master entities
   */
  default void onSelection(List<Entity> selectedEntities) {}

  /**
   * Called when a insert is performed in the master model, regardless of entity type.
   * @param insertedEntities the inserted entities
   */
  default void onInsert(List<Entity> insertedEntities) {}

  /**
   * Called when an update is performed in the master model, regardless of entity type.
   * @param updatedEntities the updated entities, mapped to their original primary keys
   */
  default void onUpdate(Map<Key, Entity> updatedEntities) {}

  /**
   * Called when delete is performed in the master model, regardless of entity type.
   * @param deletedEntities the deleted entities
   */
  default void onDelete(List<Entity> deletedEntities) {}
}
