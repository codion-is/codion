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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.component.table.FilterTableModel;
import is.codion.common.model.component.table.FilterTableSort;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * An abstract {@link EntityTableModel} implementation.
 * @param <E> the {@link EntityEditModel} type
 * @param <R> the {@link EntityEditor} type
 */
public abstract class AbstractEntityTableModel<E extends EntityEditModel<R>, R extends EntityEditor<R>> implements EntityTableModel<E, R> {

	private final FilterTableModel<Entity, Attribute<?>> filterModel;
	private final E editModel;
	private final EntityQueryModel queryModel;
	private final State removeDeleted = State.state(true);
	private final State orderQuery = State.state(ORDER_QUERY.getOrThrow());
	private final Value<OnInsert> onInsert = Value.nonNull(ON_INSERT.getOrThrow());

	//we keep a reference to this listener, since it will only be referenced via a WeakReference elsewhere
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();

	/**
	 * @param editModel the edit model
	 * @param filterModel the filter model
	 */
	protected AbstractEntityTableModel(E editModel, FilterTableModel<Entity, Attribute<?>> filterModel) {
		this(editModel, new DefaultEntityQueryModel(EntityConditionModel.builder()
						.entityType(editModel.entityType())
						.connectionProvider(editModel.connectionProvider())
						.build()), filterModel);
	}

	/**
	 * @param editModel the edit model
	 * @param filterModel the filter model
	 * @param queryModel the table query model
	 * @throws IllegalArgumentException in case the edit and query model entity types do not match
	 */
	protected AbstractEntityTableModel(E editModel, EntityQueryModel queryModel, FilterTableModel<Entity, Attribute<?>> filterModel) {
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
	public final R editor() {
		return editModel.editor();
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
	public final EntityQueryModel query() {
		return queryModel;
	}

	@Override
	public final void replace(Collection<Entity> entities) {
		items().replace(requireNonNull(entities).stream()
						.collect(toMap(identity(), identity())));
	}

	@Override
	public final void select(Collection<Entity.Key> keys) {
		selection().items().set(new SelectByKeyPredicate(requireNonNull(keys)));
	}

	@Override
	public final Items<Entity> items() {
		return filterModel.items();
	}

	@Override
	public final ColumnValues<Attribute<?>> values() {
		return filterModel.values();
	}

	@Override
	public final TableColumns<Entity, Attribute<?>> columns() {
		return filterModel.columns();
	}

	@Override
	public final TableConditionModel<Attribute<?>> filters() {
		return filterModel.filters();
	}

	@Override
	public final FilterTableSort<Entity, Attribute<?>> sort() {
		return filterModel.sort();
	}

	@Override
	public final Export<Attribute<?>> export() {
		return filterModel.export();
	}

	/**
	 * @return the underlying model
	 */
	protected FilterTableModel<Entity, Attribute<?>> filterModel() {
		return filterModel;
	}

	/**
	 * @param entityDefinition the entity definition
	 * @return {@link TableColumns} based on the visible attributes of the given entity definition
	 */
	protected static TableColumns<Entity, Attribute<?>> tableColumns(EntityDefinition entityDefinition) {
		return new EntityTableColumns(entityDefinition);
	}

	/**
	 * @param entityDefinition the entity definition
	 * @return a {@link Supplier} providing the filter condition models based on the given entity definition
	 */
	protected static Supplier<Map<Attribute<?>, ConditionModel<?>>> filterConditions(EntityDefinition entityDefinition) {
		return new EntityFilters(entityDefinition);
	}

	/**
	 * @param entityType the entity type
	 * @return a {@link Predicate} validating that items are of the given entity type
	 */
	protected static Predicate<Entity> itemValidator(EntityType entityType) {
		return new EntityItemValidator(entityType);
	}

	/**
	 * Notifies all listeners that the given rows have changed
	 * @param fromIndex the from index
	 * @param toIndex the to index
	 */
	protected abstract void onRowsUpdated(int fromIndex, int toIndex);

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
		List<Entity> includedItems = items().included().get();
		for (int i = 0; i < includedItems.size(); i++) {
			Entity entity = includedItems.get(i);
			for (Map.Entry<Entity.Key, Entity> entry : entities.entrySet()) {
				if (replace(foreignKey, entity, entry.getKey(), entry.getValue())) {
					onRowsUpdated(i, i);
				}
			}
		}
	}

	/**
	 * @return a {@link OrderBy} instance based on the sort order according to the {@link #sort()} model, an empty {@link Optional} if unsorted
	 */
	private Optional<OrderBy> orderBy() {
		List<FilterTableSort.ColumnSortOrder<Attribute<?>>> columnSortOrder = sort().columns().get().stream()
						.filter(sortOrder -> sortOrder.identifier() instanceof Column)
						.collect(toList());
		if (columnSortOrder.isEmpty()) {
			return Optional.empty();
		}
		OrderBy.Builder builder = OrderBy.builder();
		columnSortOrder.forEach(sortOrder -> {
			switch (sortOrder.sortOrder()) {
				case ASCENDING:
					builder.ascending((Column<?>) sortOrder.identifier());
					break;
				case DESCENDING:
					builder.descending((Column<?>) sortOrder.identifier());
					break;
				default:
					break;
			}
		});

		return Optional.of(builder.build());
	}

	private void bindEvents() {
		editModel.editor().events().after().insert().addConsumer(this::onInsert);
		editModel.editor().events().after().update().addConsumer(this::onUpdate);
		editModel.editor().events().after().delete().addConsumer(this::onDelete);
		editModel.editor().entity().addConsumer(this::onEntityChanged);
		selection().item().addConsumer(this::onSelectionChanged);

		orderQuery.addConsumer(enabled ->
						queryModel.orderBy().set(enabled ? orderBy().orElse(null) : null));
		sort().observer().addListener(() ->
						queryModel.orderBy().set(orderQuery.is() ? orderBy().orElse(null) : null));
		entityDefinition().foreignKeys().get().stream()
						.map(ForeignKey::referencedType)
						.distinct()
						.map(PersistenceEvents::persistenceEvents)
						.map(PersistenceEvents::updated)
						.forEach(updated -> updated.addWeakConsumer(updateListener));
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		Collection<Entity> entitiesToAdd = insertedEntities.stream()
						.filter(entity -> entity.type().equals(entityType()))
						.collect(toList());
		OnInsert onInsertAction = onInsert.getOrThrow();
		if (onInsertAction != OnInsert.DO_NOTHING && !entitiesToAdd.isEmpty()) {
			selection().clear();
			items().included().add(onInsertAction == OnInsert.PREPEND ? 0 : items().included().size(), entitiesToAdd);
		}
	}

	private void onUpdate(Map<Entity, Entity> updatedEntities) {
		items().replace(updatedEntities.entrySet().stream()
						.collect(toMap(entry -> entry.getKey().copy()
										.builder()
										.originalPrimaryKey()
										.build(), Map.Entry::getValue)));
		syncEditor(updatedEntities);
	}

	// Syncs the editor's active entity when the currently selected row is among those updated, so the edit form
	// reflects a value changed via the table (bulk or inline editing). A modified editor is left
	// untouched so unsaved edits are never overwritten.
	private void syncEditor(Map<Entity, Entity> updatedEntities) {
		EntityEditor.EditorEntity editorEntity = editModel.editor().entity();
		if (editorEntity.modified().is()) {
			return;
		}
		Entity selected = selection().item().get();
		if (selected != null && updatedEntities.values().stream()
						.anyMatch(updated -> updated.primaryKey().equals(selected.primaryKey()))
						&& !editorEntity.get().equalValues(selected)) {
			editorEntity.replace(selected);
		}
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		if (removeDeleted.is()) {
			items().remove(deletedEntities);
		}
	}

	private void onEntityChanged(Entity entity) {
		if (entity == null) {
			selection().clear();
		}
	}

	private void onSelectionChanged(@Nullable Entity selected) {
		if (selected == null) {
			editModel.editor().entity().defaults();
		}
		else {
			editModel.editor().entity().set(selected);
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

	/**
	 * An abstract {@link EntityRowEditor} implementation.
	 * @param <R> the {@link EntityEditor} type
	 */
	protected abstract static class AbstractEntityRowEditor<R extends EntityEditor<R>> implements EntityRowEditor {

		private final R editor;
		private final State enabled = State.state();
		private final Value<Editable> editable;

		protected AbstractEntityRowEditor(R editor) {
			this.editor = requireNonNull(editor);
			this.editable = Value.nonNull(new Editable() {});
		}

		@Override
		public final State enabled() {
			return enabled;
		}

		@Override
		public final Value<Editable> editable() {
			return editable;
		}

		@Override
		public final boolean editable(Entity entity, Attribute<?> attribute) {
			if (editor.settings().readOnly().is() || !editor.settings().updateEnabled().is()) {
				return false;
			}

			return EntityRowEditor.super.editable(entity, attribute);
		}

		@Override
		public final <T> void set(@Nullable T value, Entity entity, Attribute<T> attribute) {
			if (!editable(entity, attribute)) {
				throw new IllegalStateException("Attribute is not editable, entity: " + entity + ", attribute: " + attribute);
			}
			Entity copy = entity.copy().mutable();
			editor.value(attribute).set(copy, value);
			try {
				if (copy.modified()) {
					editor.update(singleton(copy));
				}
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
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

	private static final class EntityTableColumns implements TableColumns<Entity, Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final List<Attribute<?>> identifiers;

		private EntityTableColumns(EntityDefinition entityDefinition) {
			this.entityDefinition = entityDefinition;
			this.identifiers = unmodifiableList(entityDefinition.attributes().definitions().stream()
							.filter(attributeDefinition -> !attributeDefinition.hidden())
							.map(AttributeDefinition::attribute)
							.collect(toList()));
			if (this.identifiers.isEmpty()) {
				throw new IllegalArgumentException("No visible attributes found for entity '" +
								entityDefinition.type() + "'. Ensure at least one attribute has a caption() " +
								"defined to make it visible in table views. Attributes without captions are " +
								"hidden by default.");
			}
		}

		@Override
		public List<Attribute<?>> identifiers() {
			return identifiers;
		}

		@Override
		public String caption(Attribute<?> identifier) {
			return entityDefinition.attributes().definition(identifier).caption();
		}

		@Override
		public Optional<String> description(Attribute<?> identifier) {
			return entityDefinition.attributes().definition(identifier).description();
		}

		@Override
		public Class<?> columnClass(Attribute<?> identifier) {
			return requireNonNull(identifier).type().valueClass();
		}

		@Override
		public @Nullable Object value(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).get(attribute);
		}

		@Override
		public String formatted(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).formatted(attribute);
		}

		@Override
		public Comparator<?> comparator(Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				return entityDefinition.foreignKeys().referencedBy((ForeignKey) attribute).comparator();
			}

			return entityDefinition.attributes().definition(attribute).comparator();
		}
	}

	private static final class EntityFilters implements Supplier<Map<Attribute<?>, ConditionModel<?>>> {

		private final EntityDefinition entityDefinition;

		private EntityFilters(EntityDefinition entityDefinition) {
			this.entityDefinition = requireNonNull(entityDefinition);
		}

		@Override
		public Map<Attribute<?>, ConditionModel<?>> get() {
			return entityDefinition.attributes().definitions().stream()
							.filter(EntityFilters::include)
							.collect(toMap(AttributeDefinition::attribute, EntityFilters::condition));
		}

		private static ConditionModel<?> condition(AttributeDefinition<?> definition) {
			if (useStringCondition(definition)) {
				// Covers foreign keys
				return ConditionModel.builder()
								.valueClass(String.class)
								.build();
			}

			return valueCondition((ValueAttributeDefinition<?>) definition);
		}

		private static <T> ConditionModel<T> valueCondition(ValueAttributeDefinition<T> definition) {
			return ConditionModel.builder()
							.valueClass(definition.attribute().type().valueClass())
							.format(definition.format().orElse(null))
							.dateTimePattern(definition.dateTimePattern().orElse(null))
							.operands(new AttributeOperands<>(definition))
							.build();
		}

		private static boolean include(AttributeDefinition<?> definition) {
			return !definition.hidden() && (definition instanceof ForeignKeyDefinition || definition instanceof ValueAttributeDefinition<?>);
		}

		private static boolean useStringCondition(AttributeDefinition<?> definition) {
			return definition.attribute().type().isEntity() || // entities
							itemBased(definition) || // items
							!Comparable.class.isAssignableFrom(definition.attribute().type().valueClass()); // non-comparables
		}

		private static boolean itemBased(AttributeDefinition<?> definition) {
			return definition instanceof ValueAttributeDefinition<?> && !((ValueAttributeDefinition<?>) definition).items().isEmpty();
		}
	}

	private static final class EntityItemValidator implements Predicate<Entity> {

		private final EntityType entityType;

		private EntityItemValidator(EntityType entityType) {
			this.entityType = requireNonNull(entityType);
		}

		@Override
		public boolean test(Entity entity) {
			return entity.type().equals(entityType);
		}
	}
}
