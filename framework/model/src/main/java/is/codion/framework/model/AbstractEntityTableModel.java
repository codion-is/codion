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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.FilterModel;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractEntityTableModel<E extends AbstractEntityEditModel> implements EntityTableModel<E> {

	private final FilterModel<Entity> tableModel;
	private final E editModel;
	private final EntityTableConditionModel conditionModel;
	private final ValueSet<Attribute<?>> attributes = ValueSet.<Attribute<?>>builder()
					.validator(new AttributeValidator())
					.build();
	private final State conditionRequired = State.state();
	private final State handleEditEvents = State.builder()
					.consumer(new HandleEditEventsChanged())
					.build();
	private final State editable = State.state();
	private final Value<Integer> limit = Value.value();
	private final Value<OrderBy> orderBy;
	private final State removeDeleted = State.state(true);
	private final Value<OnInsert> onInsert = Value.builder()
					.nonNull(EntityTableModel.ON_INSERT.get())
					.build();

	private final State conditionChanged = State.state();
	private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();

	private EntityConnection.Select refreshCondition;

	protected AbstractEntityTableModel(E editModel, EntityTableConditionModel conditionModel,
																		 Function<AbstractEntityTableModel<E>, FilterModel<Entity>> tableModel) {
		this.editModel = requireNonNull(editModel);
		this.conditionModel = requireNonNull(conditionModel);
		if (!editModel.entityType().equals(conditionModel.entityType())) {
			throw new IllegalArgumentException("Entity type mismatch, edit model: " + editModel.entities()
							+ ", condition model: " + conditionModel.entityType());
		}
		this.tableModel = tableModel.apply(this);
		this.orderBy = createOrderBy();
		this.refreshCondition = createSelect(conditionModel);
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
	public final ValueSet<Attribute<?>> attributes() {
		return attributes;
	}

	@Override
	public final Value<Integer> limit() {
		return limit;
	}

	@Override
	public final Value<OrderBy> orderBy() {
		return orderBy;
	}

	@Override
	public final State conditionRequired() {
		return conditionRequired;
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
	public final EntityTableConditionModel conditionModel() {
		return conditionModel;
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
	public final State editable() {
		return editable;
	}

	@Override
	public Object backgroundColor(int row, Attribute<?> attribute) {
		return entityDefinition().backgroundColorProvider().color(tableModel.items().visible().itemAt(row), requireNonNull(attribute));
	}

	@Override
	public Object foregroundColor(int row, Attribute<?> attribute) {
		return entityDefinition().foregroundColorProvider().color(tableModel.items().visible().itemAt(row), requireNonNull(attribute));
	}

	@Override
	public final Optional<Entity> find(Entity.Key primaryKey) {
		requireNonNull(primaryKey);
		return items().visible().get().stream()
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	@Override
	public final int indexOf(Entity.Key primaryKey) {
		return find(primaryKey)
						.map(items().visible()::indexOf)
						.orElse(-1);
	}

	@Override
	public final void replace(Collection<Entity> entities) {
		replaceEntitiesByKey(Entity.primaryKeyMap(entities));
	}

	@Override
	public final void refresh(Collection<Entity.Key> keys) {
		try {
			replace(connection().select(keys));
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final void select(Collection<Entity.Key> keys) {
		selectionModel().selectedItems().set(new SelectByKeyPredicate(requireNonNull(keys, "keys")));
	}

	@Override
	public final Collection<Entity> find(Collection<Entity.Key> keys) {
		requireNonNull(keys, "keys");
		return items().get().stream()
						.filter(entity -> keys.contains(entity.primaryKey()))
						.collect(toList());
	}

	@Override
	public final Collection<Entity> deleteSelected() throws DatabaseException {
		return editModel().delete(selectionModel().selectedItems().get());
	}

	@Override
	public final StateObserver conditionChanged() {
		return conditionChanged.observer();
	}

	@Override
	public final Observer<?> selectionChanged() {
		return selectionModel().selectedIndexes().observer();
	}

	@Override
	public final Value<Predicate<Entity>> visiblePredicate() {
		return tableModel.visiblePredicate();
	}

	@Override
	public final Items<Entity> items() {
		return tableModel.items();
	}

	@Override
	public final Refresher<Entity> refresher() {
		return tableModel.refresher();
	}

	@Override
	public final void refresh() {
		tableModel.refresh();
	}

	@Override
	public final void refreshThen(Consumer<Collection<Entity>> afterRefresh) {
		tableModel.refreshThen(afterRefresh);
	}

	@Override
	public final void clear() {
		tableModel.clear();
	}

	@Override
	public final int rowCount() {
		return tableModel.items().visible().get().size();
	}

	@Override
	public final void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
		requireNonNull(foreignKey, "foreignKey");
		requireNonNull(foreignKeyValues, "foreignKeyValues");
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
	 * @return the underlying table model
	 */
	protected FilterModel<Entity> tableModel() {
		return tableModel;
	}

	/**
	 * Queries the data used to populate this EntityTableModel when it is refreshed.
	 * This method should take into account the where and having conditions
	 * ({@link EntityTableConditionModel#where(Conjunction)}, {@link EntityTableConditionModel#having(Conjunction)}),
	 * order by clause ({@link #orderBy()}), the limit ({@link #limit()}) and select attributes
	 * ({@link #attributes()}) when querying.
	 * @return entities selected from the database according to the query condition.
	 * @see #conditionRequired()
	 * @see #conditionEnabled(EntityTableConditionModel)
	 * @see EntityTableConditionModel#where(Conjunction)
	 * @see EntityTableConditionModel#having(Conjunction)
	 */
	protected Collection<Entity> refreshItems() {
		try {
			return queryItems();
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Notifies all listeners that the given rows have changed
	 * @param fromIndex the from index
	 * @param toIndex the to index
	 */
	protected abstract void onRowsUpdated(int fromIndex, int toIndex);

	/**
	 * It can be necessary to prevent the user from selecting too much data, when working with a large dataset.
	 * This can be done by enabling the {@link #conditionRequired()}, which prevents a refresh as long as this
	 * method returns {@code false}. This default implementation simply returns {@link EntityTableConditionModel#enabled()}.
	 * Override for a more fine grained control, such as requiring a specific column condition to be enabled.
	 * @param conditionModel the table condition model
	 * @return true if enough conditions are enabled for a safe refresh
	 * @see #conditionRequired()
	 */
	protected boolean conditionEnabled(EntityTableConditionModel conditionModel) {
		return conditionModel.enabled();
	}

	private void bindEvents() {
		editModel().afterInsert().addConsumer(this::onInsert);
		editModel().afterUpdate().addConsumer(this::onUpdate);
		editModel().afterDelete().addConsumer(this::onDelete);
		editModel().entity().addConsumer(this::onEntityChanged);
		selectionModel().selectedItem().addConsumer(editModel().entity()::set);
		conditionModel.conditionChanged().addListener(this::onConditionChanged);
	}

	private List<Entity> queryItems() throws DatabaseException {
		EntityConnection.Select select = createSelect(conditionModel);
		if (conditionRequired.get() && !conditionEnabled(conditionModel)) {
			updateRefreshSelect(select);

			return emptyList();
		}
		List<Entity> items = connection().select(select);
		updateRefreshSelect(select);

		return items;
	}

	private void updateRefreshSelect(EntityConnection.Select select) {
		refreshCondition = select;
		conditionChanged.set(false);
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entitiesToAdd = insertedEntities.stream()
						.filter(entity -> entity.entityType().equals(entityType()))
						.collect(toList());
		if (!onInsert.isEqualTo(OnInsert.DO_NOTHING) && !entitiesToAdd.isEmpty()) {
			if (!selectionModel().selectionEmpty().get()) {
				selectionModel().clearSelection();
			}
			switch (onInsert.get()) {
				case ADD_TOP:
					addItemsAt(0, entitiesToAdd);
					break;
				case ADD_TOP_SORTED:
					addItemsAtSorted(0, entitiesToAdd);
					break;
				case ADD_BOTTOM:
					addItemsAt(items().visible().get().size(), entitiesToAdd);
					break;
				case ADD_BOTTOM_SORTED:
					addItemsAtSorted(items().visible().get().size(), entitiesToAdd);
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
			removeItems(deletedEntities);
		}
	}

	private void onEntityChanged(Entity entity) {
		if (entity == null && selectionModel().selectionEmpty().not().get()) {
			selectionModel().clearSelection();
		}
	}

	private void onConditionChanged() {
		conditionChanged.set(!Objects.equals(refreshCondition, createSelect(conditionModel)));
	}

	/**
	 * Replace the entities identified by the Entity.Key map keys with their respective value.
	 * Note that this does not trigger {@link #filterItems()}, that must be done explicitly.
	 * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
	 */
	private void replaceEntitiesByKey(Map<Entity.Key, Entity> entitiesByKey) {
		Map<Entity.Key, Integer> keyIndexes = keyIndexes(new HashSet<>(entitiesByKey.keySet()));
		keyIndexes.forEach((key, index) -> setItemAt(index, entitiesByKey.remove(key)));
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

	private EntityConnection.Select createSelect(EntityTableConditionModel conditionModel) {
		return EntityConnection.Select.where(conditionModel.where(Conjunction.AND))
						.having(conditionModel.having(Conjunction.AND))
						.attributes(attributes().get())
						.limit(limit().get())
						.orderBy(orderBy.get())
						.build();
	}

	private Value<OrderBy> createOrderBy() {
		return entityDefinition().orderBy()
						.map(entityOrderBy -> Value.builder()
										.nonNull(entityOrderBy)
										.build())
						.orElse(Value.value());
	}

	private static boolean replace(ForeignKey foreignKey, Entity entity, Entity foreignKeyValue) {
		Entity currentForeignKeyValue = entity.entity(foreignKey);
		if (currentForeignKeyValue != null && currentForeignKeyValue.equals(foreignKeyValue)) {
			entity.put(foreignKey, foreignKeyValue.immutable());

			return true;
		}

		return false;
	}

	private class AttributeValidator implements Value.Validator<Set<Attribute<?>>> {

		@Override
		public void validate(Set<Attribute<?>> attributes) {
			for (Attribute<?> attribute : attributes) {
				if (!attribute.entityType().equals(entityType())) {
					throw new IllegalArgumentException(attribute + " is not part of entity:  " + entityType());
				}
			}
		}
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
								EntityEditEvents.addUpdateListener(entityType, updateListener));
			}
			else {
				entityTypes().forEach(entityType ->
								EntityEditEvents.removeUpdateListener(entityType, updateListener));
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
