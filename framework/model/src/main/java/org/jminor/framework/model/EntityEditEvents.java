/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.entity.Entity;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A central event hub for listening for entity inserts, updates and deletes.
 * Uses {@link java.lang.ref.WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 * {@link EntityEditModel} uses this to post its events.
 * @see EntityEditModel#POST_EDIT_EVENTS
 */
public final class EntityEditEvents {

  private static final DefaultEntityEditObserver EDIT_OBSERVER = new DefaultEntityEditObserver();

  private EntityEditEvents() {}

  /**
   * Adds a insert listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.addInsertListener(entityId, listener);
  }

  /**
   * Adds a update listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
    EDIT_OBSERVER.addUpdateListener(entityId, listener);
  }

  /**
   * Adds a delete listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.addDeleteListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.removeInsertListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
    EDIT_OBSERVER.removeUpdateListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.removeDeleteListener(entityId, listener);
  }

  /**
   * Notifies insert
   * @param insertedEntities the inserted entities
   */
  public static void notifyInserted(final List<Entity> insertedEntities) {
    EDIT_OBSERVER.notifyInserted(requireNonNull(insertedEntities));
  }

  /**
   * Notifies update
   * @param updatedEntities the updated entities mapped to their original primary key
   */
  public static void notifyUpdated(final Map<Entity.Key, Entity> updatedEntities) {
    EDIT_OBSERVER.notifyUpdated(requireNonNull(updatedEntities));
  }

  /**
   * Notifies delete
   * @param deletedEntities the deleted entities
   */
  public static void notifyDeleted(final List<Entity> deletedEntities) {
    EDIT_OBSERVER.notifyDeleted(requireNonNull(deletedEntities));
  }
}
