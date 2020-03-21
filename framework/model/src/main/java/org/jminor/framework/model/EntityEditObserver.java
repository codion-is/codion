/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.entity.Entity;

import java.util.List;
import java.util.Map;

/**
 * An event hub for listening for entity inserts, updates and deletes.
 * Uses {@link java.lang.ref.WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 */
public interface EntityEditObserver {

  /**
   * Adds a insert listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addInsertListener(String entityId, EventDataListener<List<Entity>> listener);

  /**
   * Adds a update listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addUpdateListener(String entityId, EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * Adds a delete listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addDeleteListener(String entityId, EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeInsertListener(String entityId, EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeUpdateListener(String entityId, EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeDeleteListener(String entityId, EventDataListener<List<Entity>> listener);

  /**
   * Notifies insert
   * @param insertedEntities the inserted entities
   */
  void notifyInserted(List<Entity> insertedEntities);

  /**
   * Notifies update
   * @param updatedEntities the updated entities mapped to their original primary key
   */
  void notifyUpdated(Map<Entity.Key, Entity> updatedEntities);

  /**
   * Notifies delete
   * @param deletedEntities the deleted entities
   */
  void notifyDeleted(List<Entity> deletedEntities);
}
