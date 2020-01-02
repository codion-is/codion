/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
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
 * A default {@link EntityEditObserver} implementation.
 */
final class DefaultEntityEditObserver implements EntityEditObserver {

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
  public void notifyInserted(final List<Entity> insertedEntities) {
    requireNonNull(insertedEntities);
    mapToEntityId(insertedEntities).forEach((entityId, inserted) -> {
      final WeakObserver<List<Entity>> event = insertEvents.get(entityId);
      if (event != null) {
        event.onEvent(inserted);
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public void notifyUpdated(final Map<Entity.Key, Entity> updatedEntities) {
    requireNonNull(updatedEntities);
    map(updatedEntities.entrySet(),
            entry -> entry.getKey().getEntityId()).forEach((entityId, updated) -> {
      final Map<Entity.Key, Entity> updateMap = new HashMap<>();
      updated.forEach(entry -> updateMap.put(entry.getKey(), entry.getValue()));
      final WeakObserver<Map<Entity.Key, Entity>> event = updateEvents.get(entityId);
      if (event != null) {
        event.onEvent(updateMap);
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public void notifyDeleted(final List<Entity> deletedEntities) {
    requireNonNull(deletedEntities);
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
