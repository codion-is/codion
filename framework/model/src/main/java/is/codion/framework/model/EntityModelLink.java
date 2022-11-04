/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

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
public interface EntityModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * @return the detail model
   */
  M detailModel();

  /**
   * Called when the selection changes in the master model
   * @param selectedEntities the selected master entities
   */
  default void onSelection(List<Entity> selectedEntities) {}

  /**
   * Called when a insert is performed in the master model
   * @param insertedEntities the inserted entities
   */
  default void onInsert(List<Entity> insertedEntities) {}

  /**
   * Called when an update is performed in the master model
   * @param updatedEntities the updated entities, mapped to their original primary keys
   */
  default void onUpdate(Map<Key, Entity> updatedEntities) {}

  /**
   * Called when a delete is performed in the master model
   * @param deletedEntities the deleted entities
   */
  default void onDelete(List<Entity> deletedEntities) {}
}
