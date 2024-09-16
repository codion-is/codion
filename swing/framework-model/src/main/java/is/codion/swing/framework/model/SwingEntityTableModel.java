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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel implements EntityTableModel<SwingEntityEditModel>, FilterTableModel<Entity, Attribute<?>> {

	/**
	 * Caches java.awt.Color instances parsed from hex strings via {@link #toColor(Object)}
	 */
	private static final Map<String, Color> COLOR_CACHE = new ConcurrentHashMap<>();

	private final FilterTableModel<Entity, Attribute<?>> tableModel;
	private final SwingEntityEditModel editModel;
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

	private Select refreshCondition;

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param entityType the entityType
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new SwingEntityEditModel(entityType, connectionProvider));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param conditionModel the table condition model
	 */
	public SwingEntityTableModel(EntityTableConditionModel conditionModel) {
		this(new SwingEntityEditModel(requireNonNull(conditionModel).entityType(), conditionModel.connectionProvider()), conditionModel);
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel) {
		this(editModel, entityTableConditionModel(editModel.entityType(), editModel.connectionProvider(),
						new SwingEntityConditionModelFactory(editModel.connectionProvider())));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 * @param conditionModel the table condition model
	 * @throws IllegalArgumentException in case the edit model and condition model entity type is not the same
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel, EntityTableConditionModel conditionModel) {
		this.editModel = requireNonNull(editModel);
		this.conditionModel = requireNonNull(conditionModel);
		if (!editModel.entityType().equals(conditionModel.entityType())) {
			throw new IllegalArgumentException("Entity type mismatch, edit model: " + editModel.entities()
							+ ", condition model: " + conditionModel.entityType());
		}
		this.tableModel = createTableModel(editModel.entityDefinition());
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
	public final <C extends SwingEntityEditModel> C editModel() {
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

	/**
	 * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
	 * @param rowIndex the row to edit
	 * @param modelColumnIndex the model index of the column to edit
	 * @return true if the cell is editable
	 * @see #setValueAt(Object, int, int)
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int modelColumnIndex) {
		if (!editable.get() || editModel.readOnly().get() || !editModel.updateEnabled().get()) {
			return false;
		}
		Attribute<?> attribute = columns().identifier(modelColumnIndex);
		if (attribute instanceof ForeignKey) {
			return entityDefinition().foreignKeys().updatable((ForeignKey) attribute);
		}

		AttributeDefinition<?> attributeDefinition = entityDefinition().attributes().definition(attribute);

		return attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).updatable();
	}

	/**
	 * Sets the value in the given cell and updates the underlying Entity.
	 * @param value the new value
	 * @param rowIndex the row whose value is to be changed
	 * @param modelColumnIndex the model index of the column to be changed
	 */
	@Override
	public final void setValueAt(Object value, int rowIndex, int modelColumnIndex) {
		if (!editable.get() || editModel.readOnly().get() || !editModel.updateEnabled().get()) {
			throw new IllegalStateException("This table model is readOnly or has disabled update");
		}
		Entity entity = itemAt(rowIndex).copy();
		entity.put((Attribute<Object>) columns().identifier(modelColumnIndex), value);
		try {
			if (entity.modified()) {
				editModel.update(singletonList(entity));
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Color backgroundColor(int row, Attribute<?> attribute) {
		requireNonNull(attribute);
		Object color = entityDefinition().backgroundColorProvider().color(itemAt(row), attribute);

		return color == null ? null : toColor(color);
	}

	@Override
	public Color foregroundColor(int row, Attribute<?> attribute) {
		requireNonNull(attribute);
		Object color = entityDefinition().foregroundColorProvider().color(itemAt(row), attribute);

		return color == null ? null : toColor(color);
	}

	@Override
	public final Optional<Entity> find(Entity.Key primaryKey) {
		requireNonNull(primaryKey);
		return visibleItems().stream()
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	@Override
	public final int indexOf(Entity.Key primaryKey) {
		return find(primaryKey)
						.map(this::indexOf)
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
	public final void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
		requireNonNull(foreignKey, "foreignKey");
		requireNonNull(foreignKeyValues, "foreignKeyValues");
		entityDefinition().foreignKeys().definition(foreignKey);
		for (Entity entity : filteredItems()) {
			for (Entity foreignKeyValue : foreignKeyValues) {
				replace(foreignKey, entity, foreignKeyValue);
			}
		}
		for (int i = 0; i < visibleItems().size(); i++) {
			Entity entity = visibleItems().get(i);
			for (Entity foreignKeyValue : foreignKeyValues) {
				if (replace(foreignKey, entity, foreignKeyValue)) {
					fireTableRowsUpdated(i, i);
				}
			}
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
		return editModel.delete(selectionModel().selectedItems().get());
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
	public final void filterItems() {
		tableModel.filterItems();
	}

	@Override
	public final Value<Predicate<Entity>> includeCondition() {
		return tableModel.includeCondition();
	}

	@Override
	public Observable<Collection<Entity>> items() {
		return tableModel.items();
	}

	@Override
	public final List<Entity> visibleItems() {
		return tableModel.visibleItems();
	}

	@Override
	public final Collection<Entity> filteredItems() {
		return tableModel.filteredItems();
	}

	@Override
	public final int visibleCount() {
		return tableModel.visibleCount();
	}

	@Override
	public final int filteredCount() {
		return tableModel.filteredCount();
	}

	@Override
	public final boolean containsItem(Entity item) {
		return tableModel.containsItem(item);
	}

	@Override
	public final boolean visible(Entity item) {
		return tableModel.visible(item);
	}

	@Override
	public final boolean filtered(Entity item) {
		return tableModel.filtered(item);
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
		return getRowCount();
	}

	@Override
	public final int getRowCount() {
		return tableModel.getRowCount();
	}

	@Override
	public final int indexOf(Entity item) {
		return tableModel.indexOf(item);
	}

	@Override
	public final Entity itemAt(int rowIndex) {
		return tableModel.itemAt(rowIndex);
	}

	@Override
	public final Object getValueAt(int rowIndex, int columnIndex) {
		return tableModel.getValueAt(rowIndex, columnIndex);
	}

	@Override
	public final String getStringAt(int rowIndex, Attribute<?> attribute) {
		return tableModel.getStringAt(rowIndex, attribute);
	}

	@Override
	public final void addItems(Collection<Entity> items) {
		tableModel.addItems(items);
	}

	@Override
	public final void addItemsSorted(Collection<Entity> items) {
		tableModel.addItemsSorted(items);
	}

	@Override
	public final void addItemsAt(int index, Collection<Entity> items) {
		tableModel.addItemsAt(index, items);
	}

	@Override
	public final void addItemsAtSorted(int index, Collection<Entity> items) {
		tableModel.addItemsAtSorted(index, items);
	}

	@Override
	public final void addItem(Entity item) {
		tableModel.addItem(item);
	}

	@Override
	public final void addItemAt(int index, Entity item) {
		tableModel.addItemAt(index, item);
	}

	@Override
	public final void addItemSorted(Entity item) {
		tableModel.addItemSorted(item);
	}

	@Override
	public final void setItemAt(int index, Entity item) {
		tableModel.setItemAt(index, item);
	}

	@Override
	public final void removeItems(Collection<Entity> items) {
		tableModel.removeItems(items);
	}

	@Override
	public final void removeItem(Entity item) {
		tableModel.removeItem(item);
	}

	@Override
	public final Entity removeItemAt(int index) {
		return tableModel.removeItemAt(index);
	}

	@Override
	public final List<Entity> removeItems(int fromIndex, int toIndex) {
		return tableModel.removeItems(fromIndex, toIndex);
	}

	@Override
	public final void fireTableDataChanged() {
		tableModel.fireTableDataChanged();
	}

	@Override
	public final void fireTableRowsUpdated(int fromIndex, int toIndex) {
		tableModel.fireTableRowsUpdated(fromIndex, toIndex);
	}

	@Override
	public final <T> Collection<T> values(Attribute<?> attribute) {
		return tableModel.values(attribute);
	}

	@Override
	public final Class<?> getColumnClass(Attribute<?> attribute) {
		return tableModel.getColumnClass(attribute);
	}

	@Override
	public final <T> Collection<T> selectedValues(Attribute<?> attribute) {
		return tableModel.selectedValues(attribute);
	}

	@Override
	public final Value<RefreshStrategy> refreshStrategy() {
		return tableModel.refreshStrategy();
	}

	@Override
	public final void sortItems() {
		tableModel.sortItems();
	}

	@Override
	public final FilterTableSelectionModel<Entity> selectionModel() {
		return tableModel.selectionModel();
	}

	@Override
	public final TableConditionModel<Attribute<?>> filterModel() {
		return tableModel.filterModel();
	}

	@Override
	public final int getColumnCount() {
		return tableModel.getColumnCount();
	}

	@Override
	public final String getColumnName(int columnIndex) {
		return tableModel.getColumnName(columnIndex);
	}

	@Override
	public final Class<?> getColumnClass(int columnIndex) {
		return tableModel.getColumnClass(columnIndex);
	}

	@Override
	public final Observer<?> dataChanged() {
		return tableModel.dataChanged();
	}

	@Override
	public final Observer<?> cleared() {
		return tableModel.cleared();
	}

	@Override
	public final void addTableModelListener(TableModelListener listener) {
		tableModel.addTableModelListener(listener);
	}

	@Override
	public final void removeTableModelListener(TableModelListener listener) {
		tableModel.removeTableModelListener(listener);
	}

	@Override
	public final Columns<Entity, Attribute<?>> columns() {
		return tableModel.columns();
	}

	@Override
	public final Value<Comparator<Entity>> comparator() {
		return tableModel.comparator();
	}

	/**
	 * @param entities the entities to display
	 * @param connectionProvider the connection provider
	 * @return a static {@link SwingEntityTableModel} instance containing the given entities
	 * @throws IllegalArgumentException in case {@code entities} is empty
	 */
	public static SwingEntityTableModel tableModel(Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
		if (requireNonNull(entities).isEmpty()) {
			throw new IllegalArgumentException("One or more entities is required for a static table model");
		}

		SwingEntityTableModel tableModel = new SwingEntityTableModel(entities.iterator().next().entityType(), connectionProvider) {
			@Override
			protected Collection<Entity> refreshItems() {
				return entities;
			}
		};
		tableModel.refresh();

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

	/**
	 * Returns a {@link java.awt.Color} instance from the given Object.
	 * {@link java.awt.Color} instances are returned as-is, but instances of
	 * {@link java.lang.String} are assumed to be in HEX format (f.ex: #ffff00" or #00ff00)
	 * and are parsed with {@link Color#decode(String)}. Colors parsed from Strings are cached.
	 * Override to support other representations.
	 * @param color the object representing the color.
	 * @return a {@link java.awt.Color} instance based on the given Object
	 * @throws IllegalArgumentException in case the representation is not supported
	 * @throws NullPointerException in case color is null
	 */
	protected Color toColor(Object color) {
		requireNonNull(color);
		if (color instanceof Color) {
			return (Color) color;
		}
		if (color instanceof String) {
			return COLOR_CACHE.computeIfAbsent((String) color, Color::decode);
		}

		throw new IllegalArgumentException("Unsupported Color representation: " + color);
	}

	private void bindEvents() {
		conditionModel.conditionChanged().addListener(() -> onConditionChanged(createSelect(conditionModel)));
		editModel.afterInsert().addConsumer(this::onInsert);
		editModel.afterUpdate().addConsumer(this::onUpdate);
		editModel.afterDelete().addConsumer(this::onDelete);
		editModel.entityChanged().addConsumer(this::onEntityChanged);
		selectionModel().selectedItem().addConsumer(editModel::set);
		addTableModelListener(this::onTableModelEvent);
	}

	private List<Entity> queryItems() throws DatabaseException {
		Select select = createSelect(conditionModel);
		if (conditionRequired.get() && !conditionEnabled(conditionModel)) {
			updateRefreshSelect(select);

			return emptyList();
		}
		List<Entity> items = connection().select(select);
		updateRefreshSelect(select);

		return items;
	}

	private void updateRefreshSelect(Select select) {
		refreshCondition = select;
		conditionChanged.set(false);
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entitiesToAdd = insertedEntities.stream()
						.filter(entity -> entity.entityType().equals(entityType()))
						.collect(toList());
		if (!onInsert.isEqualTo(OnInsert.DO_NOTHING) && !entitiesToAdd.isEmpty()) {
			if (!selectionModel().isSelectionEmpty()) {
				selectionModel().clearSelection();
			}
			switch (onInsert.get()) {
				case ADD_TOP:
					tableModel.addItemsAt(0, entitiesToAdd);
					break;
				case ADD_TOP_SORTED:
					tableModel.addItemsAtSorted(0, entitiesToAdd);
					break;
				case ADD_BOTTOM:
					tableModel.addItemsAt(visibleCount(), entitiesToAdd);
					break;
				case ADD_BOTTOM_SORTED:
					tableModel.addItemsAtSorted(visibleCount(), entitiesToAdd);
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
		if (entity == null && !selectionModel().isSelectionEmpty()) {
			selectionModel().clearSelection();
		}
	}

	private void onTableModelEvent(TableModelEvent tableModelEvent) {
		//if the selected row is updated via the table model, refresh the one in the edit model
		if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == selectionModel().selectedIndex().get().intValue()) {
			editModel.set(selectionModel().selectedItem().get());
		}
	}

	private void onConditionChanged(Select condition) {
		conditionChanged.set(!Objects.equals(refreshCondition, condition));
	}

	/**
	 * Replace the entities identified by the Entity.Key map keys with their respective value.
	 * Note that this does not trigger {@link #filterItems()}, that must be done explicitly.
	 * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
	 */
	private void replaceEntitiesByKey(Map<Entity.Key, Entity> entitiesByKey) {
		Map<Entity.Key, Integer> keyIndexes = keyIndexes(new HashSet<>(entitiesByKey.keySet()));
		keyIndexes.forEach((key, index) -> tableModel.setItemAt(index, entitiesByKey.remove(key)));
		if (!entitiesByKey.isEmpty()) {
			filteredItems().forEach(item -> {
				Entity replacement = entitiesByKey.remove(item.primaryKey());
				if (replacement != null) {
					item.set(replacement);
				}
			});
		}
	}

	private Map<Entity.Key, Integer> keyIndexes(Set<Entity.Key> keys) {
		List<Entity> visibleItems = visibleItems();
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

	private Select createSelect(EntityTableConditionModel conditionModel) {
		return Select.where(conditionModel.where(Conjunction.AND))
						.having(conditionModel.having(Conjunction.AND))
						.attributes(attributes().get())
						.limit(limit().get())
						.orderBy(orderBy.get())
						.build();
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

	private Value<OrderBy> createOrderBy() {
		return entityDefinition().orderBy()
						.map(entityOrderBy -> Value.builder()
										.nonNull(entityOrderBy)
										.build())
						.orElse(Value.value());
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

	private FilterTableModel<Entity, Attribute<?>> createTableModel(EntityDefinition entityDefinition) {
		return FilterTableModel.builder(new EntityTableColumns(entityDefinition))
						.filterModelFactory(new EntityFilterModelFactory(entityDefinition))
						.items(new EntityItems(this))
						.validator(new EntityItemValidator(entityDefinition.entityType()))
						.build();
	}

	private static final class EntityTableColumns implements Columns<Entity, Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final List<Attribute<?>> identifiers;

		private EntityTableColumns(EntityDefinition entityDefinition) {
			this.entityDefinition = entityDefinition;
			this.identifiers = unmodifiableList(entityDefinition.attributes().definitions().stream()
							.filter(attributeDefinition -> !attributeDefinition.hidden())
							.map(AttributeDefinition::attribute)
							.collect(toList()));
		}

		@Override
		public List<Attribute<?>> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> columnClass(Attribute<?> identifier) {
			return requireNonNull(identifier).type().valueClass();
		}

		@Override
		public Object value(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).get(attribute);
		}

		@Override
		public String string(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).string(attribute);
		}

		@Override
		public Comparator<?> comparator(Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				return entityDefinition.foreignKeys().referencedBy((ForeignKey) attribute).comparator();
			}

			return entityDefinition.attributes().definition(attribute).comparator();
		}
	}

	private static final class EntityFilterModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

		private final EntityDefinition entityDefinition;

		private EntityFilterModelFactory(EntityDefinition entityDefinition) {
			this.entityDefinition = requireNonNull(entityDefinition);
		}

		@Override
		public Optional<ColumnConditionModel<Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
			if (!include(attribute)) {
				return Optional.empty();
			}

			AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
			ColumnConditionModel<?, ?> model;
			if (useStringCondition(attribute, attributeDefinition)) {
				model = ColumnConditionModel.builder(attribute, String.class).build();

				return Optional.of((ColumnConditionModel<Attribute<?>, ?>) model);
			}

			model = ColumnConditionModel.builder(attribute, attribute.type().valueClass())
							.format(attributeDefinition.format())
							.dateTimePattern(attributeDefinition.dateTimePattern())
							.build();

			return Optional.of((ColumnConditionModel<Attribute<?>, ?>) model);
		}

		private boolean include(Attribute<?> attribute) {
			AttributeDefinition<?> definition = entityDefinition.attributes().definition(attribute);
			if (definition.hidden()) {
				return false;
			}

			return !(attribute instanceof ForeignKey);
		}

		private static boolean useStringCondition(Attribute<?> attribute, AttributeDefinition<?> attributeDefinition) {
			return attribute.type().isEntity() || // entities
							!attributeDefinition.items().isEmpty() || // items
							!Comparable.class.isAssignableFrom(attribute.type().valueClass()); // non-comparables
		}
	}

	private static final class EntityItems implements Supplier<Collection<Entity>> {

		private final SwingEntityTableModel tableModel;

		private EntityItems(SwingEntityTableModel tableModel) {
			this.tableModel = requireNonNull(tableModel);
		}

		@Override
		public Collection<Entity> get() {
			return tableModel.refreshItems();
		}
	}

	private static final class EntityItemValidator implements Predicate<Entity> {

		private final EntityType entityType;

		private EntityItemValidator(EntityType entityType) {
			this.entityType = requireNonNull(entityType);
		}

		@Override
		public boolean test(Entity entity) {
			return entity.entityType().equals(entityType);
		}
	}
}