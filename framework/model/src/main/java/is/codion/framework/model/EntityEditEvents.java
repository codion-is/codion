/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Util.map;
import static is.codion.framework.domain.entity.Entities.mapToType;
import static java.util.Objects.requireNonNull;

/**
 * A central event hub for listening for entity inserts, updates and deletes.
 * You must keep a live reference to any listeners added in order to prevent
 * them from being garbage collected, since listeners are added via a {@link java.lang.ref.WeakReference}.
 * {@link EntityEditModel} uses this to post its events.
 * @see EntityEditModel#POST_EDIT_EVENTS
 */
public final class EntityEditEvents {

  private static final EntityEditObserver EDIT_OBSERVER = new EntityEditObserver();

  private EntityEditEvents() {}

  /**
   * Adds a insert listener, notified each time entities of the given type are inserted.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addInsertListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.addInsertListener(entityType, listener);
  }

  /**
   * Adds a update listener, notified each time entities of the given type are updated.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(final EntityType<?> entityType, final EventDataListener<Map<Key, Entity>> listener) {
    EDIT_OBSERVER.addUpdateListener(entityType, listener);
  }

  /**
   * Adds a delete listener, notified each time entities of the given type are deleted.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.addDeleteListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.removeInsertListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final EntityType<?> entityType, final EventDataListener<Map<Key, Entity>> listener) {
    EDIT_OBSERVER.removeUpdateListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_OBSERVER.removeDeleteListener(entityType, listener);
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
  public static void notifyUpdated(final Map<Key, Entity> updatedEntities) {
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

    private final Map<EntityType<?>, WeakObserver<List<Entity>>> insertEvents = new ConcurrentHashMap<>();
    private final Map<EntityType<?>, WeakObserver<Map<Key, Entity>>> updateEvents = new ConcurrentHashMap<>();
    private final Map<EntityType<?>, WeakObserver<List<Entity>>> deleteEvents = new ConcurrentHashMap<>();

    private void addInsertListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
      getInsertObserver(entityType).addDataListener(listener);
    }

    private void removeInsertListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
      getInsertObserver(entityType).removeDataListener(listener);
    }

    private void addUpdateListener(final EntityType<?> entityType, final EventDataListener<Map<Key, Entity>> listener) {
      getUpdateObserver(entityType).addDataListener(listener);
    }

    private void removeUpdateListener(final EntityType<?> entityType, final EventDataListener<Map<Key, Entity>> listener) {
      getUpdateObserver(entityType).removeDataListener(listener);
    }

    private void addDeleteListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
      getDeleteObserver(entityType).addDataListener(listener);
    }

    private void removeDeleteListener(final EntityType<?> entityType, final EventDataListener<List<Entity>> listener) {
      getDeleteObserver(entityType).removeDataListener(listener);
    }

    private void notifyInserted(final List<Entity> insertedEntities) {
      mapToType(insertedEntities).forEach((entityType, inserted) -> {
        final WeakObserver<List<Entity>> event = insertEvents.get(entityType);
        if (event != null) {
          event.onEvent(inserted);
        }
      });
    }

    private void notifyUpdated(final Map<Key, Entity> updatedEntities) {
      map(updatedEntities.entrySet(), entry -> entry.getKey().getEntityType()).forEach((entityType, updated) -> {
        final Map<Key, Entity> updateMap = new HashMap<>();
        updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
        final WeakObserver<Map<Key, Entity>> event = updateEvents.get(entityType);
        if (event != null) {
          event.onEvent(updateMap);
        }
      });
    }

    private void notifyDeleted(final List<Entity> deletedEntities) {
      mapToType(deletedEntities).forEach((entityType, entities) -> {
        final WeakObserver<List<Entity>> event = deleteEvents.get(entityType);
        if (event != null) {
          event.onEvent(entities);
        }
      });
    }

    private WeakObserver<List<Entity>> getInsertObserver(final EntityType<?> entityType) {
      return insertEvents.computeIfAbsent(requireNonNull(entityType), type -> new WeakObserver<>());
    }

    private WeakObserver<Map<Key, Entity>> getUpdateObserver(final EntityType<?> entityType) {
      return updateEvents.computeIfAbsent(requireNonNull(entityType), type -> new WeakObserver<>());
    }

    private WeakObserver<List<Entity>> getDeleteObserver(final EntityType<?> entityType) {
      return deleteEvents.computeIfAbsent(requireNonNull(entityType), type -> new WeakObserver<>());
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
