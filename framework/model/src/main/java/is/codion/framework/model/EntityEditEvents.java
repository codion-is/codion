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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
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
	 * Adds an insert consumer, notified each time entities of the given type are inserted.
	 * Note that you have to keep a live reference to the consumer instance,
	 * otherwise it will be garbage collected, due to a weak reference.
	 * @param entityType the type of entity to listen for
	 * @param consumer the consumer
	 */
	public static void addInsertConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
		EDIT_LISTENER.addInsertConsumer(entityType, consumer);
	}

	/**
	 * Adds an update consumer, notified each time entities of the given type are updated.
	 * Note that you have to keep a live reference to the consumer instance,
	 * otherwise it will be garbage collected, due to a weak reference.
	 * @param entityType the type of entity to listen for
	 * @param consumer the consumer
	 */
	public static void addUpdateConsumer(EntityType entityType, Consumer<Map<Entity.Key, Entity>> consumer) {
		EDIT_LISTENER.addUpdateConsumer(entityType, consumer);
	}

	/**
	 * Adds a delete consumer, notified each time entities of the given type are deleted.
	 * Note that you have to keep a live reference to the consumer instance,
	 * otherwise it will be garbage collected, due to a weak reference.
	 * @param entityType the type of entity to listen for
	 * @param consumer the consumer
	 */
	public static void addDeleteConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
		EDIT_LISTENER.addDeleteConsumer(entityType, consumer);
	}

	/**
	 * Removes the given consumer
	 * @param entityType the entityType
	 * @param consumer the consumer to remove
	 */
	public static void removeInsertConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
		EDIT_LISTENER.removeInsertConsumer(entityType, consumer);
	}

	/**
	 * Removes the given consumer
	 * @param entityType the entityType
	 * @param consumer the consumer to remove
	 */
	public static void removeUpdateConsumer(EntityType entityType, Consumer<Map<Entity.Key, Entity>> consumer) {
		EDIT_LISTENER.removeUpdateConsumer(entityType, consumer);
	}

	/**
	 * Removes the given consumer
	 * @param entityType the entityType
	 * @param consumer the consumer to remove
	 */
	public static void removeDeleteConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
		EDIT_LISTENER.removeDeleteConsumer(entityType, consumer);
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

		private final Map<EntityType, Consumers<Collection<Entity>>> insertConsumers = new ConcurrentHashMap<>();
		private final Map<EntityType, Consumers<Map<Entity.Key, Entity>>> updateConsumers = new ConcurrentHashMap<>();
		private final Map<EntityType, Consumers<Collection<Entity>>> deleteConsumers = new ConcurrentHashMap<>();

		private void addInsertConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
			insertConsumers(entityType).addConsumer(consumer);
		}

		private void removeInsertConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
			insertConsumers(entityType).removeConsumer(consumer);
		}

		private void addUpdateConsumer(EntityType entityType, Consumer<Map<Entity.Key, Entity>> consumer) {
			updateConsumers(entityType).addConsumer(consumer);
		}

		private void removeUpdateConsumer(EntityType entityType, Consumer<Map<Entity.Key, Entity>> consumer) {
			updateConsumers(entityType).removeConsumer(consumer);
		}

		private void addDeleteConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
			deleteConsumers(entityType).addConsumer(consumer);
		}

		private void removeDeleteConsumer(EntityType entityType, Consumer<Collection<Entity>> consumer) {
			deleteConsumers(entityType).removeConsumer(consumer);
		}

		private void notifyInserted(Collection<Entity> inserted) {
			mapToType(inserted).forEach(this::notifyInserted);
		}

		private void notifyInserted(EntityType entityType, Collection<Entity> inserted) {
			Consumers<Collection<Entity>> consumers = insertConsumers.get(entityType);
			if (consumers != null) {
				consumers.onEvent(inserted);
			}
		}

		private void notifyUpdated(Map<Entity.Key, Entity> updated) {
			updated.entrySet()
							.stream()
							.collect(groupingBy(entry -> entry.getKey().entityType(), LinkedHashMap::new, toList()))
							.forEach(this::notifyUpdated);
		}

		private void notifyUpdated(EntityType entityType, List<Map.Entry<Entity.Key, Entity>> updated) {
			Consumers<Map<Entity.Key, Entity>> consumers = updateConsumers.get(entityType);
			if (consumers != null) {
				consumers.onEvent(updated.stream()
								.collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
			}
		}

		private void notifyDeleted(Collection<Entity> deleted) {
			mapToType(deleted).forEach(this::notifyDeleted);
		}

		private void notifyDeleted(EntityType entityType, Collection<Entity> deleted) {
			Consumers<Collection<Entity>> consumers = deleteConsumers.get(entityType);
			if (consumers != null) {
				consumers.onEvent(deleted);
			}
		}

		private Consumers<Collection<Entity>> insertConsumers(EntityType entityType) {
			return insertConsumers.computeIfAbsent(requireNonNull(entityType), type -> new Consumers<>());
		}

		private Consumers<Map<Entity.Key, Entity>> updateConsumers(EntityType entityType) {
			return updateConsumers.computeIfAbsent(requireNonNull(entityType), type -> new Consumers<>());
		}

		private Consumers<Collection<Entity>> deleteConsumers(EntityType entityType) {
			return deleteConsumers.computeIfAbsent(requireNonNull(entityType), type -> new Consumers<>());
		}

		private static final class Consumers<T> {

			private final List<WeakReference<Consumer<T>>> consumerReferences = new ArrayList<>();

			private synchronized void onEvent(T data) {
				requireNonNull(data);
				Iterator<WeakReference<Consumer<T>>> iterator = consumerReferences.iterator();
				while (iterator.hasNext()) {
					Consumer<T> consumer = iterator.next().get();
					if (consumer == null) {
						iterator.remove();
					}
					else {
						consumer.accept(data);
					}
				}
			}

			private synchronized void addConsumer(Consumer<T> consumer) {
				requireNonNull(consumer);
				for (WeakReference<Consumer<T>> reference : consumerReferences) {
					if (reference.get() == consumer) {
						return;
					}
				}
				consumerReferences.add(new WeakReference<>(consumer));
			}

			private synchronized void removeConsumer(Consumer<T> consumer) {
				requireNonNull(consumer);
				consumerReferences.removeIf(reference -> reference.get() == null || reference.get() == consumer);
			}
		}
	}
}
