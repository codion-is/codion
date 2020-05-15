/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Util.map;
import static is.codion.framework.domain.entity.Entities.mapToEntityId;
import static java.util.Objects.requireNonNull;

/**
 * A central event hub for listening for entity inserts, updates and deletes.
 * Uses {@link java.lang.ref.WeakReference} so adding a listener does not prevent it from being garbage collected, so keep
 * a live reference to any listeners in order to keep them active.
 * {@link EntityEditModel} uses this to post its events.
 * @see EntityEditModel#POST_EDIT_EVENTS
 */
public final class EntityEditEvents {

  private static final EntityEditObserver EDIT_OBSERVER = new EntityEditObserver();

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
   * @param entityId the  entityId
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.removeInsertListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the  entityId
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
    EDIT_OBSERVER.removeUpdateListener(entityId, listener);
  }

  /**
   * Removes the given listener
   * @param entityId the  entityId
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

  private static final class EntityEditObserver {

    private final Map<String, WeakObserver<List<Entity>>> insertEvents = new ConcurrentHashMap<>();
    private final Map<String, WeakObserver<Map<Entity.Key, Entity>>> updateEvents = new ConcurrentHashMap<>();
    private final Map<String, WeakObserver<List<Entity>>> deleteEvents = new ConcurrentHashMap<>();

    private void addInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
      getInsertObserver(entityId).addDataListener(listener);
    }

    private void removeInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
      getInsertObserver(entityId).removeDataListener(listener);
    }

    private void addUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
      getUpdateObserver(entityId).addDataListener(listener);
    }

    private void removeUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
      getUpdateObserver(entityId).removeDataListener(listener);
    }

    private void addDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener) {
      getDeleteObserver(entityId).addDataListener(listener);
    }

    private void removeDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener) {
      getDeleteObserver(entityId).removeDataListener(listener);
    }

    private void notifyInserted(final List<Entity> insertedEntities) {
      mapToEntityId(insertedEntities).forEach((entityId, inserted) -> {
        final WeakObserver<List<Entity>> event = insertEvents.get(entityId);
        if (event != null) {
          event.onEvent(inserted);
        }
      });
    }

    private void notifyUpdated(final Map<Entity.Key, Entity> updatedEntities) {
      map(updatedEntities.entrySet(), entry -> entry.getKey().getEntityId()).forEach((entityId, updated) -> {
        final Map<Entity.Key, Entity> updateMap = new HashMap<>();
        updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
        final WeakObserver<Map<Entity.Key, Entity>> event = updateEvents.get(entityId);
        if (event != null) {
          event.onEvent(updateMap);
        }
      });
    }

    private void notifyDeleted(final List<Entity> deletedEntities) {
      mapToEntityId(deletedEntities).forEach((entityId, entities) -> {
        final WeakObserver<List<Entity>> event = deleteEvents.get(entityId);
        if (event != null) {
          event.onEvent(entities);
        }
      });
    }

    private WeakObserver<List<Entity>> getInsertObserver(final String entityId) {
      return insertEvents.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
    }

    private WeakObserver<Map<Entity.Key, Entity>> getUpdateObserver(final String entityId) {
      return updateEvents.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
    }

    private WeakObserver<List<Entity>> getDeleteObserver(final String entityId) {
      return deleteEvents.computeIfAbsent(requireNonNull(entityId), eId -> new WeakObserver<>());
    }

    private static final class WeakObserver<T> {

      private final List<WeakReference<EventDataListener<T>>> dataListeners = new ArrayList<>();

      private synchronized void onEvent(final T data) {
        requireNonNull(data);
        final Iterator<WeakReference<EventDataListener<T>>> iterator = dataListeners.iterator();
        while (iterator.hasNext()) {
          final EventDataListener<T> listener = iterator.next().get();
          if (listener == null) {
            iterator.remove();
          }
          else {
            listener.onEvent(data);
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
}
