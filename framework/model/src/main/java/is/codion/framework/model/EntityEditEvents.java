/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static is.codion.framework.domain.entity.Entity.mapToType;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

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
  public static void addInsertListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
    EDIT_LISTENER.addInsertListener(entityType, listener);
  }

  /**
   * Adds an update listener, notified each time entities of the given type are updated.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addUpdateListener(EntityType entityType, Consumer<Map<Entity.Key, Entity>> listener) {
    EDIT_LISTENER.addUpdateListener(entityType, listener);
  }

  /**
   * Adds a delete listener, notified each time entities of the given type are deleted.
   * Note that you have to keep a live reference to the listener instance,
   * otherwise it will be garbage collected, due to a weak reference.
   * @param entityType the type of entity to listen for
   * @param listener the listener
   */
  public static void addDeleteListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
    EDIT_LISTENER.addDeleteListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeInsertListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
    EDIT_LISTENER.removeInsertListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeUpdateListener(EntityType entityType, Consumer<Map<Entity.Key, Entity>> listener) {
    EDIT_LISTENER.removeUpdateListener(entityType, listener);
  }

  /**
   * Removes the given listener
   * @param entityType the entityType
   * @param listener the listener to remove
   */
  public static void removeDeleteListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
    EDIT_LISTENER.removeDeleteListener(entityType, listener);
  }

  /**
   * Notifies insert
   * @param insertedEntities the inserted entities
   */
  public static void notifyInserted(Collection<Entity> insertedEntities) {
    EDIT_LISTENER.notifyInserted(requireNonNull(insertedEntities));
  }

  /**
   * Notifies update
   * @param updatedEntities the updated entities mapped to their original primary key
   */
  public static void notifyUpdated(Map<Entity.Key, Entity> updatedEntities) {
    EDIT_LISTENER.notifyUpdated(requireNonNull(updatedEntities));
  }

  /**
   * Notifies delete
   * @param deletedEntities the deleted entities
   */
  public static void notifyDeleted(Collection<Entity> deletedEntities) {
    EDIT_LISTENER.notifyDeleted(requireNonNull(deletedEntities));
  }

  private static final class EntityEditListener {

    private final Map<EntityType, Listeners<Collection<Entity>>> insertListeners = new ConcurrentHashMap<>();
    private final Map<EntityType, Listeners<Map<Entity.Key, Entity>>> updateListeners = new ConcurrentHashMap<>();
    private final Map<EntityType, Listeners<Collection<Entity>>> deleteListeners = new ConcurrentHashMap<>();

    private void addInsertListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
      insertListeners(entityType).addDataListener(listener);
    }

    private void removeInsertListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
      insertListeners(entityType).removeDataListener(listener);
    }

    private void addUpdateListener(EntityType entityType, Consumer<Map<Entity.Key, Entity>> listener) {
      updateListeners(entityType).addDataListener(listener);
    }

    private void removeUpdateListener(EntityType entityType, Consumer<Map<Entity.Key, Entity>> listener) {
      updateListeners(entityType).removeDataListener(listener);
    }

    private void addDeleteListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
      deleteListeners(entityType).addDataListener(listener);
    }

    private void removeDeleteListener(EntityType entityType, Consumer<Collection<Entity>> listener) {
      deleteListeners(entityType).removeDataListener(listener);
    }

    private void notifyInserted(Collection<Entity> inserted) {
      mapToType(inserted).forEach(this::notifyInserted);
    }

    private void notifyInserted(EntityType entityType, Collection<Entity> inserted) {
      Listeners<Collection<Entity>> listeners = insertListeners.get(entityType);
      if (listeners != null) {
        listeners.onEvent(inserted);
      }
    }

    private void notifyUpdated(Map<Entity.Key, Entity> updated) {
      updated.entrySet()
              .stream()
              .collect(groupingBy(entry -> entry.getKey().entityType(), LinkedHashMap::new, toList()))
              .forEach(this::notifyUpdated);
    }

    private void notifyUpdated(EntityType entityType, List<Map.Entry<Entity.Key, Entity>> updated) {
      Listeners<Map<Entity.Key, Entity>> listeners = updateListeners.get(entityType);
      if (listeners != null) {
        listeners.onEvent(updated.stream()
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
      }
    }

    private void notifyDeleted(Collection<Entity> deleted) {
      mapToType(deleted).forEach(this::notifyDeleted);
    }

    private void notifyDeleted(EntityType entityType, Collection<Entity> deleted) {
      Listeners<Collection<Entity>> listeners = deleteListeners.get(entityType);
      if (listeners != null) {
        listeners.onEvent(deleted);
      }
    }

    private Listeners<Collection<Entity>> insertListeners(EntityType entityType) {
      return insertListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private Listeners<Map<Entity.Key, Entity>> updateListeners(EntityType entityType) {
      return updateListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private Listeners<Collection<Entity>> deleteListeners(EntityType entityType) {
      return deleteListeners.computeIfAbsent(requireNonNull(entityType), type -> new Listeners<>());
    }

    private static final class Listeners<T> {

      private final List<WeakReference<Consumer<T>>> listenerReferences = new ArrayList<>();

      private synchronized void onEvent(T data) {
        requireNonNull(data);
        Iterator<WeakReference<Consumer<T>>> iterator = listenerReferences.iterator();
        while (iterator.hasNext()) {
          Consumer<T> listener = iterator.next().get();
          if (listener == null) {
            iterator.remove();
          }
          else {
            listener.accept(data);
          }
        }
      }

      private synchronized void addDataListener(Consumer<T> listener) {
        requireNonNull(listener);
        for (WeakReference<Consumer<T>> reference : listenerReferences) {
          if (reference.get() == listener) {
            return;
          }
        }
        listenerReferences.add(new WeakReference<>(listener));
      }

      private synchronized void removeDataListener(Consumer<T> listener) {
        requireNonNull(listener);
        listenerReferences.removeIf(reference -> reference.get() == null || reference.get() == listener);
      }
    }
  }
}
