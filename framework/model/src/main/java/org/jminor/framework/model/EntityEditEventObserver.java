/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.Entity;

import java.util.List;
import java.util.Map;

/**
 * An event hub for listening for entity inserts, updates and deletes.
 * Uses {@link java.lang.ref.WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 */
public interface EntityEditEventObserver {

  /**
   * Adds a insert listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addInsertListener(final String entityId, final EventDataListener<List<Entity>> listener);

  /**
   * Adds a update listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener);

  /**
   * Adds a delete listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  void addDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeInsertListener(final String entityId, final EventDataListener listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeUpdateListener(final String entityId, final EventDataListener listener);

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  void removeDeleteListener(final String entityId, final EventDataListener listener);

  /**
   * Notifies insert
   * @param insertedEntities the inserted entities
   */
  void notifyInserted(final List<Entity> insertedEntities);

  /**
   * Notifies update
   * @param updatedEntities the updated entities mapped to their original primary key
   */
  void notifyUpdated(final Map<Entity.Key, Entity> updatedEntities);

  /**
   * Notifies delete
   * @param deletedEntities the deleted entities
   */
  void notifyDeleted(final List<Entity> deletedEntities);
}
