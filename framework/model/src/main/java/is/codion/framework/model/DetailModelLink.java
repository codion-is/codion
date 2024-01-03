/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;

import java.util.Collection;
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
   * Controls the active status of this link. Active detail model links update and filter
   * the detail model according to the entity/entities selected in the master model.
   * @return the state controlling the active status of this detail model link
   */
  State active();

  /**
   * Called when the selection changes in the master model
   * @param selectedEntities the selected master entities
   */
  default void onSelection(Collection<Entity> selectedEntities) {}

  /**
   * Called when a insert is performed in the master model, regardless of entity type.
   * @param insertedEntities the inserted entities
   */
  default void onInsert(Collection<Entity> insertedEntities) {}

  /**
   * Called when an update is performed in the master model, regardless of entity type.
   * @param updatedEntities the updated entities, mapped to their original primary keys
   */
  default void onUpdate(Map<Entity.Key, Entity> updatedEntities) {}

  /**
   * Called when delete is performed in the master model, regardless of entity type.
   * @param deletedEntities the deleted entities
   */
  default void onDelete(Collection<Entity> deletedEntities) {}
}
