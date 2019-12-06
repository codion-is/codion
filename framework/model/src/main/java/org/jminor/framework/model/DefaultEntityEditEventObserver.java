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
 * A default {@link EntityEditEventObserver} implementation.
 */
final class DefaultEntityEditEventObserver implements EntityEditEventObserver {

  private final Map<String, WeakObserver<List<Entity>>> insertEvents = new ConcurrentHashMap<>();
  private final Map<String, WeakObserver<Map<Entity.Key, Entity>>> updateEvents = new ConcurrentHashMap<>();
  private final Map<String, WeakObserver<List<Entity>>> deleteEvents = new ConcurrentHashMap<>();

  /** {@inheritDoc} */
  @Override
  public void addInsertListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    getInsertObserver(entityId).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addUpdateListener(final String entityId, final EventDataListener<Map<Entity.Key, Entity>> listener) {
    getUpdateObserver(entityId).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addDeleteListener(final String entityId, final EventDataListener<List<Entity>> listener) {
    getDeleteObserver(entityId).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeInsertListener(final String entityId, final EventDataListener listener) {
    getInsertObserver(entityId).removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeUpdateListener(final String entityId, final EventDataListener listener) {
    getUpdateObserver(entityId).removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeDeleteListener(final String entityId, final EventDataListener listener) {
    getDeleteObserver(entityId).removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void inserted(final EntityEditModel.InsertEvent insertEvent) {
    requireNonNull(insertEvent);
    mapToEntityId(insertEvent.getInsertedEntities()).forEach((entityId, inserted) -> {
      final WeakObserver<List<Entity>> event = insertEvents.get(entityId);
      if (event != null) {
        event.fire(inserted);
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public void updated(final EntityEditModel.UpdateEvent updateEvent) {
    requireNonNull(updateEvent);
    map(updateEvent.getUpdatedEntities().entrySet(),
            entry -> entry.getKey().getEntityId()).forEach((entityId, updated) -> {
      final Map<Entity.Key, Entity> updateMap = new HashMap<>();
      updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
      final WeakObserver<Map<Entity.Key, Entity>> event = updateEvents.get(entityId);
      if (event != null) {
        event.fire(updateMap);
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public void deleted(final EntityEditModel.DeleteEvent deleteEvent) {
    requireNonNull(deleteEvent);
    mapToEntityId(deleteEvent.getDeletedEntities()).forEach((entityId, entities) -> {
      final WeakObserver<List<Entity>> event = deleteEvents.get(entityId);
      if (event != null) {
        event.fire(entities);
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
