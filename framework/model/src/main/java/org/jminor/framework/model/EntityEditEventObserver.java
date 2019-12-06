/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.Entity;

import java.util.List;
import java.util.Map;

/**
 * An event hub for listening for entity inserts, updates and deletes made via {@link EntityEditModel}s.
 * Uses {@link WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
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
   * @param insertEvent the insert event
   */
  void inserted(final EntityEditModel.InsertEvent insertEvent);

  /**
   * Notifies update
   * @param updateEvent the update event
   */
  void updated(final EntityEditModel.UpdateEvent updateEvent);

  /**
   * Notifies delete
   * @param deleteEvent the delete event
   */
  void deleted(final EntityEditModel.DeleteEvent deleteEvent);
}
