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
package is.codion.swing.framework.model.component;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.filterComboBoxModel;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultEntityComboBoxModel implements EntityComboBoxModel {

	private final FilterComboBoxModel<Entity> comboBoxModel;

	private final EntityType entityType;
	private final EntityConnectionProvider connectionProvider;

	private final Entities entities;
	private final DefaultForeignKeyFilter foreignKeyFilter;
	private final Value<Supplier<Condition>> condition;
	private final OrderBy orderBy;
	private final Collection<Attribute<?>> attributes;

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Collection<Entity>> insertListener = new InsertListener();
	private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	DefaultEntityComboBoxModel(DefaultBuilder builder) {
		this.entityType = builder.entityType;
		this.connectionProvider = builder.connectionProvider;
		this.attributes = builder.attributes;
		this.entities = connectionProvider.entities();
		this.comboBoxModel = filterComboBoxModel(this::performQuery);
		this.condition = Value.builder()
						.nonNull(builder.condition)
						.build();
		this.orderBy = builder.orderBy;
		if (orderBy != null) {
			// otherwise the sorting overrides the order by
			comboBoxModel.items().visible().comparator().clear();
		}
		if (builder.nullCaption != null) {
			setNullCaption(builder.nullCaption);
		}
		foreignKeyFilter = new DefaultForeignKeyFilter();
		comboBoxModel.selection().filterSelected().set(builder.filterSelected);
		comboBoxModel.selection().translator().set(new SelectedItemTranslator());
		comboBoxModel.items().visible().predicate().set(foreignKeyFilter.predicate);
		if (builder.handleEditEvents) {
			addEditListeners();
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [entityType: " + entityType + "]";
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public Optional<Entity> find(Entity.Key primaryKey) {
		requireNonNull(primaryKey);
		return items().get().stream()
						.filter(Objects::nonNull)
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	@Override
	public void select(Entity.Key primaryKey) {
		requireNonNull(primaryKey);
		Optional<Entity> entity = find(primaryKey);
		if (entity.isPresent()) {
			setSelectedItem(entity.get());
		}
		else {
			filteredEntity(primaryKey).ifPresent(this::setSelectedItem);
		}
	}

	/**
	 * Controls the condition supplier to use when querying data, set to null to fetch all underlying entities.
	 * @return a value controlling the condition supplier
	 */
	@Override
	public Value<Supplier<Condition>> condition() {
		return condition;
	}

	@Override
	public ForeignKeyFilter filter() {
		return foreignKeyFilter;
	}

	@Override
	public <T> Value<T> createSelectorValue(Attribute<T> attribute) {
		if (!entities.definition(entityType()).attributes().contains(attribute)) {
			throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType());
		}

		return createSelectorValue(new EntityFinder<>(attribute));
	}

	@Override
	public ComboBoxSelection<Entity> selection() {
		return comboBoxModel.selection();
	}

	@Override
	public Entity getSelectedItem() {
		return comboBoxModel.getSelectedItem();
	}

	@Override
	public <V> Value<V> createSelectorValue(ItemFinder<Entity, V> itemFinder) {
		return comboBoxModel.createSelectorValue(itemFinder);
	}

	@Override
	public ComboBoxItems<Entity> items() {
		return comboBoxModel.items();
	}

	@Override
	public Refresher<Entity> refresher() {
		return comboBoxModel.refresher();
	}

	@Override
	public void refresh() {
		comboBoxModel.refresh();
	}

	@Override
	public void refresh(Consumer<Collection<Entity>> onRefresh) {
		comboBoxModel.refresh(onRefresh);
	}

	@Override
	public void setSelectedItem(Object selectedItem) {
		comboBoxModel.setSelectedItem(selectedItem);
	}

	@Override
	public int getSize() {
		return comboBoxModel.getSize();
	}

	@Override
	public Entity getElementAt(int index) {
		return comboBoxModel.getElementAt(index);
	}

	@Override
	public void addListDataListener(ListDataListener listener) {
		comboBoxModel.addListDataListener(listener);
	}

	@Override
	public void removeListDataListener(ListDataListener listener) {
		comboBoxModel.removeListDataListener(listener);
	}

	private Collection<Entity> performQuery() {
		try {
			return connectionProvider.connection().select(where(condition.get().get())
							.attributes(attributes)
							.orderBy(orderBy)
							.build());
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	private Optional<Entity> filteredEntity(Entity.Key primaryKey) {
		return items().filtered().get().stream()
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	private void setNullCaption(String nullCaption) {
		items().nullItem().include().set(true);
		items().nullItem().set(ProxyBuilder.builder(Entity.class)
						.delegate(entities.entity(entityType))
						.method("toString", parameters -> nullCaption)
						.build());
	}

	private final class SelectedItemTranslator implements Function<Object, Entity> {

		@Override
		public Entity apply(Object itemToSelect) {
			if (itemToSelect == null) {
				return null;
			}

			if (itemToSelect instanceof Entity) {
				return find(((Entity) itemToSelect).primaryKey()).orElse((Entity) itemToSelect);
			}
			String itemToString = itemToSelect.toString();

			return items().visible().get().stream()
							.filter(visibleItem -> visibleItem != null && itemToString.equals(visibleItem.toString()))
							.findFirst()
							//item not found, select null value
							.orElse(null);
		}
	}

	private final class InsertListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> inserted) {
			inserted.forEach(items()::addItem);
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity.Key, Entity>> {

		@Override
		public void accept(Map<Entity.Key, Entity> updated) {
			updated.forEach((key, entity) -> items().replace(Entity.entity(key), entity));
		}
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			deleted.forEach(items()::removeItem);
		}
	}

	private void addEditListeners() {
		EntityEditEvents.insertObserver(entityType).addWeakConsumer(insertListener);
		EntityEditEvents.updateObserver(entityType).addWeakConsumer(updateListener);
		EntityEditEvents.deleteObserver(entityType).addWeakConsumer(deleteListener);
	}

	/**
	 * Controls the foreign key filters for a {@link EntityComboBoxModel}
	 */
	private final class DefaultForeignKeyFilter implements ForeignKeyFilter {

		private final Map<ForeignKey, Set<Entity.Key>> foreignKeyFilterKeys = new HashMap<>();
		private final Predicate<Entity> predicate = new ForeignKeyFilterPredicate();
		private final State strict = State.state(true);

		@Override
		public void set(ForeignKey foreignKey, Collection<Entity.Key> keys) {
			requireNonNull(foreignKey);
			requireNonNull(keys);
			if (keys.isEmpty()) {
				foreignKeyFilterKeys.remove(foreignKey);
			}
			else {
				foreignKeyFilterKeys.put(foreignKey, new HashSet<>(keys));
			}
			comboBoxModel.items().visible().predicate().set(predicate);
		}

		@Override
		public Collection<Entity.Key> get(ForeignKey foreignKey) {
			requireNonNull(foreignKey);

			return unmodifiableSet(foreignKeyFilterKeys.getOrDefault(foreignKey, emptySet()));
		}

		@Override
		public State strict() {
			return strict;
		}

		@Override
		public Predicate<Entity> predicate() {
			return predicate;
		}

		@Override
		public EntityComboBoxModel.Builder builder(ForeignKey foreignKey) {
			return new DefaultBuilder(foreignKey.referencedType(), connectionProvider, DefaultEntityComboBoxModel.this, requireNonNull(foreignKey))
							.includeNull(connectionProvider.entities().definition(foreignKey.entityType()).foreignKeys().definition(foreignKey).nullable());
		}

		@Override
		public void link(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
			entities.definition(entityType).foreignKeys().definition(foreignKey);
			if (!foreignKey.referencedType().equals(foreignKeyModel.entityType())) {
				throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.entityType()
								+ ", should be: " + foreignKey.referencedType());
			}
			//if foreign key filter keys have been set previously, initialize with one of those
			Collection<Entity.Key> filterKeys = get(foreignKey);
			if (!filterKeys.isEmpty()) {
				foreignKeyModel.select(filterKeys.iterator().next());
			}
			Predicate<Entity> hideAllCondition = item -> false;
			if (strict.get()) {
				comboBoxModel.items().visible().predicate().set(hideAllCondition);
			}
			foreignKeyModel.selection().item().addConsumer(selected -> {
				if (selected == null && strict.get()) {
					comboBoxModel.items().visible().predicate().set(hideAllCondition);
				}
				else {
					set(foreignKey, selected == null ? emptyList() : singletonList(selected.primaryKey()));
				}
			});
			selection().item().addConsumer(selected -> {
				if (selected != null && !selected.isNull(foreignKey)) {
					foreignKeyModel.select(selected.key(foreignKey));
				}
			});
			refresher().success().addListener(foreignKeyModel::refresh);
			// Select the correct foreign key item according to the selected item after refresh
			foreignKeyModel.refresher().success().addListener(() -> {
				Entity selected = getSelectedItem();
				if (selected != null && !selected.isNull(foreignKey)) {
					foreignKeyModel.select(selected.key(foreignKey));
				}
			});
		}

		private final class ForeignKeyFilterPredicate implements Predicate<Entity> {

			@Override
			public boolean test(Entity item) {
				for (Map.Entry<ForeignKey, Set<Entity.Key>> entry : foreignKeyFilterKeys.entrySet()) {
					Entity.Key key = item.key(entry.getKey());
					if (key == null) {
						return !strict.get();
					}
					if (!entry.getValue().contains(key)) {
						return false;
					}
				}

				return true;
			}
		}
	}

	private static final class DefaultConditionSupplier implements Supplier<Condition> {

		private final Condition condition;

		private DefaultConditionSupplier(EntityType entityType) {
			this.condition = Condition.all(entityType);
		}

		@Override
		public Condition get() {
			return condition;
		}
	}

	private static final class EntityFinder<T> implements ItemFinder<Entity, T> {

		private final Attribute<T> attribute;

		private EntityFinder(Attribute<T> attribute) {
			this.attribute = attribute;
		}

		@Override
		public T value(Entity item) {
			return item.get(attribute);
		}

		@Override
		public Predicate<Entity> predicate(T value) {
			return entity -> Objects.equals(entity.get(attribute), value);
		}
	}

	static class DefaultBuilder implements Builder {

		private final EntityType entityType;
		private final EntityConnectionProvider connectionProvider;
		private final EntityComboBoxModel filterModel;
		private final ForeignKey filterForeignKey;

		private OrderBy orderBy;
		private Supplier<Condition> condition;
		private Collection<Attribute<?>> attributes = emptyList();
		private boolean handleEditEvents = EntityComboBoxModel.HANDLE_EDIT_EVENTS.get();
		private String nullCaption;
		private boolean filterSelected = false;

		DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this(entityType, connectionProvider, null, null);
		}

		DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider, EntityComboBoxModel filterModel, ForeignKey filterForeignKey) {
			this.entityType = requireNonNull(entityType, "entityType");
			this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
			this.condition = new DefaultConditionSupplier(entityType);
			this.filterModel = filterModel;
			this.filterForeignKey = filterForeignKey;
		}

		@Override
		public Builder orderBy(OrderBy orderBy) {
			this.orderBy = requireNonNull(orderBy);
			return this;
		}

		@Override
		public Builder condition(Supplier<Condition> condition) {
			this.condition = requireNonNull(condition);
			return this;
		}

		@Override
		public Builder attributes(Collection<Attribute<?>> attributes) {
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (!attribute.entityType().equals(entityType)) {
					throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType);
				}
			}
			this.attributes = attributes;
			return this;
		}

		@Override
		public Builder includeNull(boolean includeNull) {
			return nullCaption(includeNull ? FilterComboBoxModel.NULL_CAPTION.get() : null);
		}

		@Override
		public Builder nullCaption(String nullCaption) {
			this.nullCaption = nullCaption;
			return this;
		}

		@Override
		public Builder handleEditEvents(boolean handleEditEvents) {
			this.handleEditEvents = handleEditEvents;
			return this;
		}

		@Override
		public Builder filterSelected(boolean filterSelected) {
			this.filterSelected = filterSelected;
			return this;
		}

		@Override
		public EntityComboBoxModel build() {
			DefaultEntityComboBoxModel entityComboBoxModel = new DefaultEntityComboBoxModel(this);
			if (filterModel != null) {
				filterModel.filter().link(filterForeignKey, entityComboBoxModel);
			}

			return entityComboBoxModel;
		}
	}
}
