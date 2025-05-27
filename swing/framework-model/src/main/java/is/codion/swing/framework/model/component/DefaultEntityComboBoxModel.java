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
package is.codion.swing.framework.model.component;

import is.codion.common.model.selection.SingleSelection;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.value.Value.Notify.SET;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.model.EntityEditModel.editEvents;
import static java.text.MessageFormat.format;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class DefaultEntityComboBoxModel implements EntityComboBoxModel {

	private final FilterComboBoxModel<Entity> comboBoxModel;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;

	private final DefaultFilter filter;
	private final Value<Supplier<Condition>> condition;
	private final OrderBy orderBy;
	private final Collection<Attribute<?>> attributes;

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Collection<Entity>> insertListener = new InsertListener();
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	DefaultEntityComboBoxModel(DefaultBuilder builder) {
		this.connectionProvider = builder.connectionProvider;
		this.entityDefinition = builder.entityDefinition;
		this.attributes = builder.attributes;
		this.comboBoxModel = FilterComboBoxModel.builder(this::performQuery)
						.nullItem(createNullItem(builder.nullCaption))
						// otherwise the sorting overrides the order by
						.comparator(builder.orderBy == null ? builder.comparator : null)
						.filterSelected(builder.filterSelected)
						.build();
		this.filter = new DefaultFilter();
		this.comboBoxModel.items().visible().predicate().set(filter);
		this.comboBoxModel.items().visible().predicate().addValidator(predicate -> {
			if (predicate != filter) {
				throw new UnsupportedOperationException("EntityComboBoxModel visible item predicate can only be set via filter().predicate().set()");
			}
		});
		this.condition = Value.builder()
						.<Supplier<Condition>>nonNull(new DefaultConditionSupplier(entityDefinition.type()))
						.value(builder.condition)
						.build();
		this.orderBy = builder.orderBy;
		if (builder.handleEditEvents) {
			editEvents().inserted(entityDefinition.type()).addWeakConsumer(insertListener);
			editEvents().updated(entityDefinition.type()).addWeakConsumer(updateListener);
			editEvents().deleted(entityDefinition.type()).addWeakConsumer(deleteListener);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [entityType: " + entityDefinition.type() + "]";
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public EntityDefinition entityDefinition() {
		return entityDefinition;
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
	public Filter filter() {
		return filter;
	}

	@Override
	public <T> Value<T> createSelectorValue(Attribute<T> attribute) {
		if (!entityDefinition.attributes().contains(attribute)) {
			throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityDefinition.type());
		}

		return createSelector(new EntityFinder<>(attribute));
	}

	@Override
	public SingleSelection<Entity> selection() {
		return comboBoxModel.selection();
	}

	@Override
	public Entity getSelectedItem() {
		return comboBoxModel.getSelectedItem();
	}

	@Override
	public <V> Value<V> createSelector(ItemFinder<Entity, V> itemFinder) {
		return comboBoxModel.createSelector(itemFinder);
	}

	@Override
	public ComboBoxItems<Entity> items() {
		return comboBoxModel.items();
	}

	@Override
	public Sort<Entity> sort() {
		return comboBoxModel.sort();
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
		return connectionProvider.connection().select(where(validate(condition.getOrThrow().get()))
						.attributes(attributes)
						.orderBy(orderBy)
						.build());
	}

	private Condition validate(Condition queryCondition) {
		if (queryCondition == null) {
			throw new IllegalArgumentException(format("EntityComboBoxModel condition supplier returned null: {0}", entityDefinition.type()));
		}
		if (!queryCondition.entityType().equals(entityDefinition.type())) {
			throw new IllegalArgumentException(format("EntityComboBoxModel condition supplier returned a condition for the incorrect type {0}, expecting: {1}",
							queryCondition.entityType(), entityDefinition.type()));
		}

		return queryCondition;
	}

	private Optional<Entity> filteredEntity(Entity.Key primaryKey) {
		return items().filtered().get().stream()
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	private Entity createNullItem(String nullCaption) {
		return nullCaption == null ? null : ProxyBuilder.builder(Entity.class)
						.delegate(entityDefinition.entity())
						.method("toString", parameters -> nullCaption)
						.build();
	}

	private final class InsertListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> inserted) {
			inserted.forEach(items()::add);
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			items().replace(updated.entrySet().stream()
							.collect(toMap(entry -> entry.getKey().copy().builder()
											.originalPrimaryKey()
											.build(), Map.Entry::getValue)));
		}
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			items().remove(deleted);
		}
	}

	private final class DefaultFilter implements Filter, Predicate<Entity> {

		private final Map<ForeignKey, DefaultForeignKeyFilter> foreignKeyFilters = new HashMap<>();
		private final Value<Predicate<Entity>> predicate = Value.builder()
						.<Predicate<Entity>>nullable()
						.notify(SET)
						.listener(items()::filter)
						.build();

		@Override
		public Value<Predicate<Entity>> predicate() {
			return predicate;
		}

		@Override
		public ForeignKeyFilter get(ForeignKey foreignKey) {
			entityDefinition.foreignKeys().definition(foreignKey);

			return foreignKeyFilters.computeIfAbsent(foreignKey, DefaultForeignKeyFilter::new);
		}

		@Override
		public boolean test(Entity entity) {
			for (Map.Entry<ForeignKey, DefaultForeignKeyFilter> entry : foreignKeyFilters.entrySet()) {
				if (!entry.getValue().test(entity)) {
					return false;
				}
			}

			return predicate.isNull() || predicate.getOrThrow().test(entity);
		}
	}

	private final class DefaultForeignKeyFilter implements ForeignKeyFilter, Predicate<Entity> {

		private final ForeignKey foreignKey;
		private final State strict = State.builder(true)
						.listener(items()::filter)
						.build();

		private Set<Entity.Key> foreignKeys;

		private DefaultForeignKeyFilter(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public boolean test(Entity item) {
			if (foreignKeys == null) {
				//cleared and disabled
				return true;
			}
			Entity.Key key = item.key(foreignKey);
			if (key == null || foreignKeys.isEmpty()) {
				return !strict.get();
			}

			return foreignKeys.isEmpty() || foreignKeys.contains(key);
		}

		@Override
		public void set(Entity.Key key) {
			set(singleton(requireNonNull(key)));
		}

		@Override
		public void set(Collection<Entity.Key> keys) {
			foreignKeys = unmodifiableSet(new HashSet<>(validateKeys(keys)));
			items().filter();
		}

		@Override
		public Collection<Entity.Key> get() {
			return foreignKeys == null ? emptySet() : foreignKeys;
		}

		@Override
		public void clear() {
			this.foreignKeys = null;
			items().filter();
		}

		@Override
		public State strict() {
			return strict;
		}

		@Override
		public void link(EntityComboBoxModel filterModel) {
			entityDefinition.foreignKeys().definition(foreignKey);
			if (!foreignKey.referencedType().equals(filterModel.entityDefinition().type())) {
				throw new IllegalArgumentException("EntityComboBoxModel is of type: " + filterModel.entityDefinition().type()
								+ ", should be: " + foreignKey.referencedType());
			}
			//if foreign key filter keys have been set previously, initialize with one of those
			Collection<Entity.Key> filterKeys = get();
			if (!filterKeys.isEmpty()) {
				filterModel.select(filterKeys.iterator().next());
			}
			set(filterModel.selection().item().get());
			filterModel.selection().item().addConsumer(this::set);
			selection().item().addConsumer(selected -> select(filterModel, selected));
			items().refresher().result().addListener(filterModel.items()::refresh);
			// Select the correct foreign key item according to the selected item after refresh
			filterModel.items().refresher().result().addListener(() -> select(filterModel, getSelectedItem()));
		}

		private void select(EntityComboBoxModel filterModel, Entity selected) {
			if (selected != null && !selected.isNull(foreignKey)) {
				filterModel.select(selected.key(foreignKey));
			}
		}

		private void set(Entity selected) {
			if (selected != null) {
				set(selected.primaryKey());
			}
			else if (strict.get()) {
				set(emptyList());
			}
			else {
				clear();
			}
		}

		private Collection<Entity.Key> validateKeys(Collection<Entity.Key> keys) {
			requireNonNull(keys);
			for (Entity.Key key : keys) {
				if (!key.type().equals(foreignKey.referencedType())) {
					throw new IllegalArgumentException("Key " + key + " is not of the correct type (" + foreignKey.referencedType() + ")");
				}
			}

			return keys;
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

		private final EntityDefinition entityDefinition;
		private final EntityConnectionProvider connectionProvider;

		private OrderBy orderBy;
		private Supplier<Condition> condition;
		private Comparator<Entity> comparator;
		private Collection<Attribute<?>> attributes = emptyList();
		private boolean handleEditEvents = HANDLE_EDIT_EVENTS.getOrThrow();
		private String nullCaption;
		private boolean filterSelected = false;

		DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this.connectionProvider = requireNonNull(connectionProvider);
			this.entityDefinition = connectionProvider.entities().definition(entityType);
			this.comparator = connectionProvider.entities().definition(entityType).comparator();
		}

		@Override
		public Builder orderBy(OrderBy orderBy) {
			this.orderBy = requireNonNull(orderBy);
			return this;
		}

		@Override
		public Builder comparator(Comparator<Entity> comparator) {
			this.comparator = comparator;
			return this;
		}

		@Override
		public Builder condition(Supplier<Condition> condition) {
			this.condition = condition;
			return this;
		}

		@Override
		public Builder attributes(Collection<Attribute<?>> attributes) {
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (!attribute.entityType().equals(entityDefinition.type())) {
					throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityDefinition.type());
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
			return new DefaultEntityComboBoxModel(this);
		}
	}
}
