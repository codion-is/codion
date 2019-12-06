/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.Entity;

import java.util.List;
import java.util.Map;

/**
 * A central event hub for listening for entity inserts, updates and deletes made via {@link EntityEditModel}s.
 * Uses {@link java.lang.ref.WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 */
public final class EntityEditEvents {

  private static final EntityEditEventObserver EVENT_OBSERVER = new DefaultEntityEditEventObserver();

  private EntityEditEvents() {}

  /**
   * Adds a insert listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EVENT_OBSERVER.addInsertListener(entityId, listener);
  }

  /**
   * Adds a update listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
    EVENT_OBSERVER.addUpdateListener(entityId, listener);
  }

  /**
   * Adds a delete listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(final String entityId,
                                       final EventDataListener<List<Entity>> listener) {
    EVENT_OBSERVER.addDeleteListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final String entityId, final EventDataListener listener) {
    EVENT_OBSERVER.removeInsertListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final String entityId, final EventDataListener listener) {
    EVENT_OBSERVER.removeUpdateListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(final String entityId, final EventDataListener listener) {
    EVENT_OBSERVER.removeDeleteListener(entityId, listener);
  }

  /**
   * Notifies insert
   * @param insertEvent the event representing the insert
   */
  static void inserted(final EntityEditModel.InsertEvent insertEvent) {
    EVENT_OBSERVER.inserted(insertEvent);
  }

  /**
   * Notifies update
   * @param updateEvent the event representing the insert
   */
  static void updated(final EntityEditModel.UpdateEvent updateEvent) {
    EVENT_OBSERVER.updated(updateEvent);
  }

  /**
   * Notifies delete
   * @param deleteEvent the event representing the insert
   */
  static void deleted(final EntityEditModel.DeleteEvent deleteEvent) {
    EVENT_OBSERVER.deleted(deleteEvent);
  }
}
