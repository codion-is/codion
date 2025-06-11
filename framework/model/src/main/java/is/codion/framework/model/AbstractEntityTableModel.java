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

import is.codion.common.model.filter.FilterModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.Entity.primaryKeyMap;
import static is.codion.framework.domain.entity.condition.Condition.keys;
import static is.codion.framework.model.EntityEditModel.events;
import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * An abstract {@link EntityTableModel} implementation
 * @param <E> the {@link EntityEditModel} type
 */
public abstract class AbstractEntityTableModel<E extends EntityEditModel> implements EntityTableModel<E> {

	private final FilterModel<Entity> filterModel;
	private final E editModel;
	private final EntityQueryModel queryModel;
	private final State editable = State.state();
	private final State removeDeleted = State.state(true);
	private final State orderQuery = State.state(ORDER_QUERY.getOrThrow());
	private final Value<OnInsert> onInsert = Value.nonNull(ON_INSERT.getOrThrow());

	//we keep a reference to this listener, since it will only be referenced via a WeakReference elsewhere
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();

	/**
	 * @param editModel the edit model
	 * @param filterModel the list model
	 */
	protected AbstractEntityTableModel(E editModel, FilterModel<Entity> filterModel) {
		this(editModel, filterModel, new DefaultEntityQueryModel(entityTableConditionModel(editModel.entityType(), editModel.connectionProvider())));
	}

	/**
	 * @param editModel the edit model
	 * @param filterModel the list model
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
	public final EntityType entityType() {
		return editModel.entityType();
	}

	@Override
	public final E editModel() {
		return editModel;
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
	public final State orderQuery() {
		return orderQuery;
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
		replaceEntities(primaryKeyMap(entities));
	}

	@Override
	public final void refresh(Collection<Entity.Key> keys) {
		if (!requireNonNull(keys).isEmpty()) {
			replaceEntities(connection().select(where(keys(keys))
											.attributes(queryModel.attributes().get())
											.build()).stream()
							.collect(toMap(Entity::primaryKey, identity())));
		}
	}

	@Override
	public final void select(Collection<Entity.Key> keys) {
		selection().items().set(new SelectByKeyPredicate(requireNonNull(keys)));
	}

	@Override
	public final Collection<Entity> deleteSelected() {
		return editModel.delete(selection().items().get());
	}

	/**
	 * @return the underlying model
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

	/**
	 * @return a {@link OrderBy} instance based on the sort order according to the {@link #sort()} model, an empty {@link Optional} if unsorted
	 */
	protected abstract Optional<OrderBy> orderBy();

	/**
	 * Called when entities of the type referenced by the given foreign key are updated
	 * @param foreignKey the foreign key
	 * @param entities the updated entities, mapped to their original primary key
	 */
	protected void updated(ForeignKey foreignKey, Map<Entity.Key, Entity> entities) {
		for (Entity entity : items().filtered().get()) {
			for (Map.Entry<Entity.Key, Entity> entry : entities.entrySet()) {
				replace(foreignKey, entity, entry.getKey(), entry.getValue());
			}
		}
		List<Entity> visibleItems = items().visible().get();
		for (int i = 0; i < visibleItems.size(); i++) {
			Entity entity = visibleItems.get(i);
			for (Map.Entry<Entity.Key, Entity> entry : entities.entrySet()) {
				if (replace(foreignKey, entity, entry.getKey(), entry.getValue())) {
					onRowsUpdated(i, i);
				}
			}
		}
	}

	private void bindEvents() {
		editModel.afterInsert().addConsumer(this::onInsert);
		editModel.afterUpdate().addConsumer(this::onUpdate);
		editModel.afterDelete().addConsumer(this::onDelete);
		editModel.editor().addConsumer(this::onEntityChanged);
		selection().item().addConsumer(editModel.editor()::set);

		orderQuery.addConsumer(enabled ->
						queryModel.orderBy().set(enabled ? orderBy().orElse(null) : null));
		sort().observer().addListener(() ->
						queryModel.orderBy().set(orderQuery.get() ? orderBy().orElse(null) : null));
		entityDefinition().foreignKeys().get().stream()
						.map(ForeignKey::referencedType)
						.distinct()
						.forEach(entityType ->
										events().updated(entityType).addWeakConsumer(updateListener));
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entitiesToAdd = insertedEntities.stream()
						.filter(entity -> entity.type().equals(entityType()))
						.collect(toList());
		OnInsert onInsertAction = onInsert.getOrThrow();
		if (onInsertAction != OnInsert.DO_NOTHING && !entitiesToAdd.isEmpty()) {
			selection().clear();
			items().visible().add(onInsertAction == OnInsert.PREPEND ? 0 : items().visible().count(), entitiesToAdd);
		}
	}

	private void onUpdate(Map<Entity, Entity> updatedEntities) {
		replaceEntities(updatedEntities.entrySet().stream()
						.collect(toMap(entry -> entry.getKey().originalPrimaryKey(), Map.Entry::getValue)));
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		if (removeDeleted.get()) {
			items().remove(deletedEntities);
		}
	}

	private void onEntityChanged(Entity entity) {
		if (entity == null) {
			selection().clear();
		}
	}

	private void replaceEntities(Map<Entity.Key, Entity> entities) {
		Map<Entity.Key, Entity> replacements = new HashMap<>(entities);
		VisibleItems<Entity> visibleItems = items().visible();
		List<Entity> visible = visibleItems.get();
		for (int i = 0; i < visible.size() && !replacements.isEmpty(); i++) {
			Entity replacement = replacements.remove(visible.get(i).primaryKey());
			if (replacement != null) {
				visibleItems.set(i, replacement);
			}
		}
		Iterator<Entity> filtered = items().filtered().get().iterator();
		while (filtered.hasNext() && !replacements.isEmpty()) {
			Entity entity = filtered.next();
			Entity replacement = replacements.remove(entity.primaryKey());
			if (replacement != null) {
				entity.set(replacement);
			}
		}
	}

	private static boolean replace(ForeignKey foreignKey, Entity entity, Entity.Key key, Entity foreignKeyValue) {
		Entity currentForeignKeyValue = entity.entity(foreignKey);
		if (currentForeignKeyValue != null && currentForeignKeyValue.primaryKey().equals(key)) {
			entity.set(foreignKey, foreignKeyValue.immutable());

			return true;
		}

		return false;
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Map<EntityType, Map<Entity.Key, Entity>> grouped = new HashMap<>();
			updated.forEach((beforeUpdate, afterUpdate) ->
							grouped.computeIfAbsent(beforeUpdate.type(), k -> new HashMap<>())
											.put(beforeUpdate.originalPrimaryKey(), afterUpdate));
			grouped.forEach(this::updated);
		}

		private void updated(EntityType entityType, Map<Entity.Key, Entity> entities) {
			entityDefinition().foreignKeys().get(entityType)
							.forEach(foreignKey -> AbstractEntityTableModel.this.updated(foreignKey, entities));
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
