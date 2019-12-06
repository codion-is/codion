/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.event.EventDataListener;
import org.jminor.framework.domain.Entity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.map;
import static org.jminor.framework.domain.Entities.mapToEntityId;

/**
 * A central event hub for listening for entity inserts, updates and deletes made via {@link EntityEditModel}s.
 * Uses {@link WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 */
public final class EntityEditEvents {

  private static final Map<String, WeakObserver<List<Entity>>> INSERT_EVENTS = new ConcurrentHashMap<>();
  private static final Map<String, WeakObserver<Map<Entity.Key, Entity>>> UPDATE_EVENTS = new ConcurrentHashMap<>();
  private static final Map<String, WeakObserver<List<Entity>>> DELETE_EVENTS = new ConcurrentHashMap<>();

  private EntityEditEvents() {}

  /**
   * Adds a insert listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addInsertListener(final String entityId,
                                       final EventDataListener<List<Entity>> listener) {
    getInsertObserver(entityId).addDataListener(listener);
  }

  /**
   * Adds a update listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(final String entityId,
                                       final EventDataListener<Map<Entity.Key, Entity>> listener) {
    getUpdateObserver(entityId).addDataListener(listener);
  }

  /**
   * Adds a delete listener
   * @param entityId the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(final String entityId,
                                       final EventDataListener<List<Entity>> listener) {
    getDeleteObserver(entityId).addDataListener(listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final String entityId, final EventDataListener listener) {
    getInsertObserver(entityId).removeDataListener(listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final String entityId, final EventDataListener listener) {
    getUpdateObserver(entityId).removeDataListener(listener);
  }

  /**
   * Removes the given listener
   * @param entityId the entity id
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(final String entityId, final EventDataListener listener) {
    getDeleteObserver(entityId).removeDataListener(listener);
  }

  static void inserted(final EntityEditModel.InsertEvent insertEvent) {
    mapToEntityId(insertEvent.getInsertedEntities()).forEach((entityId, inserted) -> {
      final WeakObserver<List<Entity>> event = INSERT_EVENTS.get(entityId);
      if (event != null) {
        event.fire(inserted);
      }
    });
  }

  static void updated(final EntityEditModel.UpdateEvent updateEvent) {
    map(updateEvent.getUpdatedEntities().entrySet(),
            entry -> entry.getKey().getEntityId()).forEach((entityId, updated) -> {
      final Map<Entity.Key, Entity> updateMap = new HashMap<>();
      updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
      final WeakObserver<Map<Entity.Key, Entity>> event = UPDATE_EVENTS.get(entityId);
      if (event != null) {
        event.fire(updateMap);
      }
    });
  }

  static void deleted(final EntityEditModel.DeleteEvent deleteEvent) {
    mapToEntityId(deleteEvent.getDeletedEntities()).forEach((entityId, entities) -> {
      final WeakObserver<List<Entity>> event = DELETE_EVENTS.get(entityId);
      if (event != null) {
        event.fire(entities);
      }
    });
  }

  private static WeakObserver<List<Entity>> getInsertObserver(final String entityId) {
    return INSERT_EVENTS.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
  }

  private static WeakObserver<Map<Entity.Key, Entity>> getUpdateObserver(final String entityId) {
    return UPDATE_EVENTS.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
  }

  private static WeakObserver<List<Entity>> getDeleteObserver(final String entityId) {
    return DELETE_EVENTS.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
  }

  private static final class WeakObserver<T> {

    private final List<WeakReference<EventDataListener<T>>> dataListeners = new ArrayList<>();

    private synchronized void fire(final T data) {
      requireNonNull(data);
      final Iterator<WeakReference<EventDataListener<T>>> iterator = dataListeners.iterator();
      while (iterator.hasNext()) {
        final EventDataListener<T> listener = iterator.next().get();
        if (listener == null) {
          iterator.remove();
        }
        else {
          listener.eventOccurred(data);
        }
      }
    }

    private synchronized void addDataListener(final EventDataListener<T> listener) {
      requireNonNull(listener);
      for (final WeakReference<EventDataListener<T>> reference : dataListeners) {
        if (reference.get() == listener) {
          return;
        }
      }
      dataListeners.add(new WeakReference<>(requireNonNull(listener)));
    }

    private synchronized void removeDataListener(final EventDataListener<T> listener) {
      requireNonNull(listener);
      dataListeners.removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }
}
