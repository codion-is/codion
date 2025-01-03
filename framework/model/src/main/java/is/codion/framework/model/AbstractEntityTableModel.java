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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.FilterModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static is.codion.framework.model.EntityConditionModel.entityConditionModel;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * An abstract {@link EntityTableModel} implementation
 * @param <E> the {@link EntityEditModel} type
 */
public abstract class AbstractEntityTableModel<E extends EntityEditModel> implements EntityTableModel<E> {

	private final FilterModel<Entity> filterModel;
	private final E editModel;
	private final EntityQueryModel queryModel;
	private final State handleEditEvents = State.builder()
					.consumer(new HandleEditEventsChanged())
					.build();
	private final State editable = State.state();
	private final State removeDeleted = State.state(true);
	private final Value<OnInsert> onInsert =
					Value.nonNull(EntityTableModel.ON_INSERT.getOrThrow());

	private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();

	/**
	 * @param editModel the edit model
	 * @param filterModel the filter model
	 */
	protected AbstractEntityTableModel(E editModel, FilterModel<Entity> filterModel) {
		this(editModel, filterModel, new DefaultEntityQueryModel(entityConditionModel(editModel.entityType(), editModel.connectionProvider())));
	}

	/**
	 * @param editModel the edit model
	 * @param filterModel the filter model
	 * @param queryModel the table query model
	 * @throws IllegalArgumentException in case the edit and query model entity types do not match
	 */
	protected AbstractEntityTableModel(E editModel, FilterModel<Entity> filterModel, EntityQueryModel queryModel) {
		this.editModel = requireNonNull(editModel);
		this.queryModel = requireNonNull(queryModel);
		this.filterModel = requireNonNull(filterModel);
		if (queryModel != null && !editModel.entityType().equals(queryModel.entityType())) {
			throw new IllegalArgumentException("Entity type mismatch, edit model: " +
							editModel.entities() + ", query model: " + queryModel.entityType());
		}
		bindEvents();
		handleEditEvents.set(HANDLE_EDIT_EVENTS.get());
	}

	@Override
	public final Entities entities() {
		return editModel.connectionProvider().entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return editModel.entityDefinition();
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + editModel.entityType();
	}

	@Override
	public final Value<OnInsert> onInsert() {
		return onInsert;
	}

	@Override
	public final State removeDeleted() {
		return removeDeleted;
	}

	@Override
	public final State handleEditEvents() {
		return handleEditEvents;
	}

	@Override
	public final EntityType entityType() {
		return editModel.entityType();
	}

	@Override
	public final <C extends E> C editModel() {
		return (C) editModel;
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editModel.connectionProvider();
	}

	@Override
	public final EntityConnection connection() {
		return editModel.connection();
	}

	@Override
	public final EntityQueryModel queryModel() {
		return queryModel;
	}

	@Override
	public final State editable() {
		return editable;
	}

	@Override
	public final void replace(Collection<Entity> entities) {
		replaceEntitiesByKey(Entity.primaryKeyMap(entities));
	}

	@Override
	public final void refresh(Collection<Entity.Key> keys) {
		replace(connection().select(keys));
	}

	@Override
	public final void select(Collection<Entity.Key> keys) {
		selection().items().set(new SelectByKeyPredicate(requireNonNull(keys)));
	}

	@Override
	public final Collection<Entity> deleteSelected() {
		return editModel.delete(selection().items().get());
	}

	@Override
	public final void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
		requireNonNull(foreignKey);
		requireNonNull(foreignKeyValues);
		entityDefinition().foreignKeys().definition(foreignKey);
		for (Entity entity : items().filtered().get()) {
			for (Entity foreignKeyValue : foreignKeyValues) {
				replace(foreignKey, entity, foreignKeyValue);
			}
		}
		List<Entity> visibleItems = items().visible().get();
		for (int i = 0; i < visibleItems.size(); i++) {
			Entity entity = visibleItems.get(i);
			for (Entity foreignKeyValue : foreignKeyValues) {
				if (replace(foreignKey, entity, foreignKeyValue)) {
					onRowsUpdated(i, i);
				}
			}
		}
	}

	/**
	 * @return the underlying filter model
	 */
	protected FilterModel<Entity> filterModel() {
		return filterModel;
	}

	/**
	 * Notifies all listeners that the given rows have changed
	 * @param fromIndex the from index
	 * @param toIndex the to index
	 */
	protected abstract void onRowsUpdated(int fromIndex, int toIndex);

	private void bindEvents() {
		editModel.afterInsert().addConsumer(this::onInsert);
		editModel.afterUpdate().addConsumer(this::onUpdate);
		editModel.afterDelete().addConsumer(this::onDelete);
		editModel.editor().addConsumer(this::onEntityChanged);
		selection().item().addConsumer(editModel.editor()::set);
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entitiesToAdd = insertedEntities.stream()
						.filter(entity -> entity.entityType().equals(entityType()))
						.collect(toList());
		if (!onInsert.isEqualTo(OnInsert.DO_NOTHING) && !entitiesToAdd.isEmpty()) {
			if (!selection().empty().get()) {
				selection().clear();
			}
			VisibleItems<Entity> visibleItems = items().visible();
			switch (onInsert.getOrThrow()) {
				case ADD_TOP:
					visibleItems.add(0, entitiesToAdd);
					break;
				case ADD_TOP_SORTED:
					if (visibleItems.add(0, entitiesToAdd)) {
						visibleItems.sort();
					}
					break;
				case ADD_BOTTOM:
					visibleItems.add(visibleItems.count(), entitiesToAdd);
					break;
				case ADD_BOTTOM_SORTED:
					if (visibleItems.add(visibleItems.count(), entitiesToAdd)) {
						visibleItems.sort();
					}
					break;
				default:
					break;
			}
		}
	}

	private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
		replaceEntitiesByKey(new HashMap<>(updatedEntities));
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		if (removeDeleted.get()) {
			items().remove(deletedEntities);
		}
	}

	private void onEntityChanged(Entity entity) {
		if (entity == null && selection().empty().not().get()) {
			selection().clear();
		}
	}

	/**
	 * Replace the entities identified by the Entity.Key map keys with their respective value.
	 * Note that this does not trigger {@link Items#filter()}, that must be done explicitly.
	 * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
	 */
	private void replaceEntitiesByKey(Map<Entity.Key, Entity> entitiesByKey) {
		Map<Entity.Key, Integer> keyIndexes = keyIndexes(new HashSet<>(entitiesByKey.keySet()));
		keyIndexes.forEach((key, index) -> items().visible().set(index, entitiesByKey.remove(key)));
		if (!entitiesByKey.isEmpty()) {
			items().filtered().get().forEach(item -> {
				Entity replacement = entitiesByKey.remove(item.primaryKey());
				if (replacement != null) {
					item.set(replacement);
				}
			});
		}
	}

	private Map<Entity.Key, Integer> keyIndexes(Set<Entity.Key> keys) {
		List<Entity> visibleItems = items().visible().get();
		Map<Entity.Key, Integer> keyIndexes = new HashMap<>();
		for (int index = 0; index < visibleItems.size(); index++) {
			Entity.Key primaryKey = visibleItems.get(index).primaryKey();
			if (keys.remove(primaryKey)) {
				keyIndexes.put(primaryKey, index);
				if (keys.isEmpty()) {
					break;
				}
			}
		}

		return keyIndexes;
	}

	private static boolean replace(ForeignKey foreignKey, Entity entity, Entity foreignKeyValue) {
		Entity currentForeignKeyValue = entity.entity(foreignKey);
		if (currentForeignKeyValue != null && currentForeignKeyValue.equals(foreignKeyValue)) {
			entity.put(foreignKey, foreignKeyValue.immutable());

			return true;
		}

		return false;
	}

	private final class UpdateListener implements Consumer<Map<Entity.Key, Entity>> {

		@Override
		public void accept(Map<Entity.Key, Entity> updated) {
			updated.values().stream()
							.collect(groupingBy(Entity::entityType, HashMap::new, toList()))
							.forEach(this::handleUpdate);
		}

		private void handleUpdate(EntityType entityType, List<Entity> entities) {
			if (entityType.equals(entityType())) {
				replace(entities);
			}
			entityDefinition().foreignKeys().get(entityType)
							.forEach(foreignKey -> replace(foreignKey, entities));
		}
	}

	private final class HandleEditEventsChanged implements Consumer<Boolean> {

		@Override
		public void accept(Boolean handleEditEvents) {
			if (handleEditEvents) {
				entityTypes().forEach(entityType ->
								EntityEditEvents.updateObserver(entityType).addWeakConsumer(updateListener));
			}
			else {
				entityTypes().forEach(entityType ->
								EntityEditEvents.updateObserver(entityType).removeWeakConsumer(updateListener));
			}
		}

		private Stream<EntityType> entityTypes() {
			return Stream.concat(entityDefinition().foreignKeys().get().stream()
							.map(ForeignKey::referencedType), Stream.of(entityType()));
		}
	}

	private static final class SelectByKeyPredicate implements Predicate<Entity> {

		private final List<Entity.Key> keyList;

		private SelectByKeyPredicate(Collection<Entity.Key> keys) {
			this.keyList = new ArrayList<>(keys);
		}

		@Override
		public boolean test(Entity entity) {
			if (keyList.isEmpty()) {
				return false;
			}
			int index = keyList.indexOf(entity.primaryKey());
			if (index >= 0) {
				keyList.remove(index);
				return true;
			}

			return false;
		}
	}
}
