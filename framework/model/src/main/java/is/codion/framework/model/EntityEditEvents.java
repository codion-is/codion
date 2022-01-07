/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import static is.codion.framework.domain.entity.Entity.mapToType;
import static java.util.Objects.requireNonNull;

/**
 * A central event hub for listening for entity inserts, updates and deletes.
 * You must keep a live reference to any listeners added in order to prevent
 * them from being garbage collected, since listeners are added via a {@link java.lang.ref.WeakReference}.
 * {@link EntityEditModel} uses this to post its events.
 * @see EntityEditModel#POST_EDIT_EVENTS
 */
public final class EntityEditEvents {

  private static final EntityEditListener EDIT_LISTENER = new EntityEditListener();

  private EntityEditEvents() {}

  /**
   * Adds an insert listener, notified each time entities of the given type are inserted.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addInsertListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_LISTENER.addInsertListener(entityType, listener);
  }

  /**
   * Adds an update listener, notified each time entities of the given type are updated.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(final EntityType entityType, final EventDataListener<Map<Key, Entity>> listener) {
    EDIT_LISTENER.addUpdateListener(entityType, listener);
  }

  /**
   * Adds a delete listener, notified each time entities of the given type are deleted.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_LISTENER.addDeleteListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeInsertListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_LISTENER.removeInsertListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(final EntityType entityType, final EventDataListener<Map<Key, Entity>> listener) {
    EDIT_LISTENER.removeUpdateListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
    EDIT_LISTENER.removeDeleteListener(entityType, listener);
  }

  /**
   * Notifies insert
   * @param insertedEntities the inserted entities
   */
  public static void notifyInserted(final List<Entity> insertedEntities) {
    EDIT_LISTENER.notifyInserted(requireNonNull(insertedEntities));
  }

  /**
   * Notifies update
   * @param updatedEntities the updated entities mapped to their original primary key
   */
  public static void notifyUpdated(final Map<Key, Entity> updatedEntities) {
    EDIT_LISTENER.notifyUpdated(requireNonNull(updatedEntities));
  }

  /**
   * Notifies delete
   * @param deletedEntities the deleted entities
   */
  public static void notifyDeleted(final List<Entity> deletedEntities) {
    EDIT_LISTENER.notifyDeleted(requireNonNull(deletedEntities));
  }

  private static final class EntityEditListener {

    private final Map<EntityType, Listeners<List<Entity>>> insertListeners = new ConcurrentHashMap<>();
    private final Map<EntityType, Listeners<Map<Key, Entity>>> updateListeners = new ConcurrentHashMap<>();
    private final Map<EntityType, Listeners<List<Entity>>> deleteListeners = new ConcurrentHashMap<>();

    private void addInsertListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
      getInsertListeners(entityType).addDataListener(listener);
    }

    private void removeInsertListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
      getInsertListeners(entityType).removeDataListener(listener);
    }

    private void addUpdateListener(final EntityType entityType, final EventDataListener<Map<Key, Entity>> listener) {
      getUpdateListeners(entityType).addDataListener(listener);
    }

    private void removeUpdateListener(final EntityType entityType, final EventDataListener<Map<Key, Entity>> listener) {
      getUpdateListeners(entityType).removeDataListener(listener);
    }

    private void addDeleteListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
      getDeleteListeners(entityType).addDataListener(listener);
    }

    private void removeDeleteListener(final EntityType entityType, final EventDataListener<List<Entity>> listener) {
      getDeleteListeners(entityType).removeDataListener(listener);
    }

    private void notifyInserted(final List<Entity> insertedEntities) {
      mapToType(insertedEntities).forEach((entityType, inserted) -> {
        final Listeners<List<Entity>> event = insertListeners.get(entityType);
        if (event != null) {
          event.onEvent(inserted);
        }
      });
    }

    private void notifyUpdated(final Map<Key, Entity> updatedEntities) {
      map(updatedEntities.entrySet(), entry -> entry.getKey().getEntityType()).forEach((entityType, updated) -> {
        final Map<Key, Entity> updateMap = new HashMap<>();
        updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
        final Listeners<Map<Key, Entity>> event = updateListeners.get(entityType);
        if (event != null) {
          event.onEvent(updateMap);
        }
      });
    }

    private void notifyDeleted(final List<Entity> deletedEntities) {
      mapToType(deletedEntities).forEach((entityType, entities) -> {
        final Listeners<List<Entity>> event = deleteListeners.get(entityType);
        if (event != null) {
          event.onEvent(entities);
        }
      });
    }

    private Listeners<List<Entity>> getInsertListeners(final EntityType entityType) {
      return insertListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private Listeners<Map<Key, Entity>> getUpdateListeners(final EntityType entityType) {
      return updateListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private Listeners<List<Entity>> getDeleteListeners(final EntityType entityType) {
      return deleteListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private static final class Listeners<T> {

      private final List<WeakReference<EventDataListener<T>>> listenerReferences = new ArrayList<>();

      private synchronized void onEvent(final T data) {
        requireNonNull(data);
        final Iterator<WeakReference<EventDataListener<T>>> iterator = listenerReferences.iterator();
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
        for (final WeakReference<EventDataListener<T>> reference : listenerReferences) {
          if (reference.get() == listener) {
            return;
          }
        }
        listenerReferences.add(new WeakReference<>(listener));
      }

      private synchronized void removeDataListener(final EventDataListener<T> listener) {
        requireNonNull(listener);
        listenerReferences.removeIf(reference -> reference.get() == null || reference.get() == listener);
      }
    }
  }
}
