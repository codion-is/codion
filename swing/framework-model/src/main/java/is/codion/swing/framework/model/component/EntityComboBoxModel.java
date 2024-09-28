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
package is.codion.swing.framework.model.component;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.property.PropertyValue;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
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
import java.util.ArrayList;
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
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 * @see #entityComboBoxModel(EntityType, EntityConnectionProvider)
 */
public final class EntityComboBoxModel implements FilterComboBoxModel<Entity> {

	/**
	 * Specifies whether entity combo box models handle entity edit events, by replacing updated entities and removing deleted ones
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * @see #handleEditEvents()
	 * @see is.codion.framework.model.EntityEditModel#POST_EDIT_EVENTS
	 */
	public static final PropertyValue<Boolean> HANDLE_EDIT_EVENTS =
					Configuration.booleanValue(EntityComboBoxModel.class.getName() + ".handleEditEvents", true);

	private final FilterComboBoxModel<Entity> comboBoxModel;

	private final EntityType entityType;
	private final EntityConnectionProvider connectionProvider;
	private final ValueSet<Attribute<?>> attributes = ValueSet.<Attribute<?>>builder()
					.validator(new AttributeValidator())
					.build();

	private final Entities entities;
	private final Map<ForeignKey, Set<Entity.Key>> foreignKeyFilterKeys = new HashMap<>();
	private final Predicate<Entity> foreignKeyVisiblePredicate = new ForeignKeyVisiblePredicate();
	private final Value<Supplier<Condition>> conditionSupplier;
	private final State handleEditEvents = State.builder()
					.consumer(new HandleEditEventsChanged())
					.build();
	private final State strictForeignKeyFiltering = State.state(true);
	private final Value<OrderBy> orderBy;

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Collection<Entity>> insertListener = new InsertListener();
	private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private EntityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this.comboBoxModel = FilterComboBoxModel.filterComboBoxModel();
		this.entityType = requireNonNull(entityType, "entityType");
		this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
		this.entities = connectionProvider.entities();
		this.orderBy = Value.value(this.entities.definition(entityType).orderBy().orElse(null));
		this.conditionSupplier = Value.builder()
						.nonNull((Supplier<Condition>) new DefaultConditionSupplier())
						.build();
		comboBoxModel.selection().translator().set(new SelectedItemTranslator());
		comboBoxModel.refresher().supplier().set(this::performQuery);
		comboBoxModel.items().validator().set(new ItemValidator());
		comboBoxModel.items().visible().predicate().set(foreignKeyVisiblePredicate);
		handleEditEvents.set(HANDLE_EDIT_EVENTS.get());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [entityType: " + entityType + "]";
	}

	/**
	 * @return the connection provider used by this combo box model
	 */
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	/**
	 * @return the type of the entity this combo box model is based on
	 */
	public EntityType entityType() {
		return entityType;
	}

	/**
	 * Enables the null item and sets the null item caption.
	 * @param nullCaption the null item caption
	 * @throws NullPointerException in case {@code nullCaption} is null
	 * @see ComboBoxItems#nullItem()
	 */
	public void setNullCaption(String nullCaption) {
		requireNonNull(nullCaption, "nullCaption");
		items().nullItem().include().set(true);
		items().nullItem().set(ProxyBuilder.builder(Entity.class)
						.delegate(entities.entity(entityType))
						.method("toString", parameters -> nullCaption)
						.build());
	}

	/**
	 * Controls the attributes to include when selecting the entities to populate this model with.
	 * Note that the primary key attribute values are always included.
	 * An empty Collection indicates that all attributes should be selected.
	 * @return the {@link ValueSet} controlling the attributes to select, an empty {@link ValueSet} indicating all available attributes
	 */
	public ValueSet<Attribute<?>> attributes() {
		return attributes;
	}

	/**
	 * @return the {@link State} controlling whether this combo box model should handle entity edit events, by adding inserted items,
	 * updating any updated items and removing deleted ones
	 * @see EntityEditEvents
	 */
	public State handleEditEvents() {
		return handleEditEvents;
	}

	/**
	 * @param primaryKey the primary key of the entity to fetch from this model
	 * @return the entity with the given key if found in the model, an empty Optional otherwise
	 */
	public Optional<Entity> find(Entity.Key primaryKey) {
		requireNonNull(primaryKey);
		return items().get().stream()
						.filter(Objects::nonNull)
						.filter(entity -> entity.primaryKey().equals(primaryKey))
						.findFirst();
	}

	/**
	 * Selects the entity with the given primary key, whether filtered or visible.
	 * If the entity is not available in the model this method returns silently without changing the selection.
	 * @param primaryKey the primary key of the entity to select
	 */
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
	public Value<Supplier<Condition>> condition() {
		return conditionSupplier;
	}

	/**
	 * Controls the order by to use when selecting entities for this model.
	 * Note that in order for this to have an effect, you must disable sorting
	 * by setting the sort comparator to null via {@link VisibleItems#comparator()}
	 * @return the {@link Value} controlling the {@link OrderBy}
	 * @see VisibleItems#comparator()
	 */
	public Value<OrderBy> orderBy() {
		return orderBy;
	}

	/**
	 * Use this method to retrieve the default foreign key filter visible predicate if you
	 * want to add a custom {@link Predicate} to this model via {@link VisibleItems#predicate()}.
	 * <pre>
	 * {@code
	 *   Predicate fkPredicate = model.foreignKeyVisiblePredicate();
	 *   model.items().visiblePredicate().set(item -&gt; fkPredicate.test(item) &amp;&amp; ...);
	 * }
	 * </pre>
	 * @return the {@link Predicate} based on the foreign key filter entities
	 * @see #filterByForeignKey(ForeignKey, Collection)
	 */
	public Predicate<Entity> foreignKeyVisiblePredicate() {
		return foreignKeyVisiblePredicate;
	}

	/**
	 * Filters this combo box model so that only items referencing the given keys via the given foreign key are visible.
	 * Note that this uses the {@link VisibleItems#predicate()} and replaces any previously set prediate.
	 * @param foreignKey the foreign key
	 * @param keys the keys, an empty Collection to clear the filter
	 * @see VisibleItems#predicate()
	 */
	public void filterByForeignKey(ForeignKey foreignKey, Collection<Entity.Key> keys) {
		requireNonNull(foreignKey);
		requireNonNull(keys);
		if (keys.isEmpty()) {
			foreignKeyFilterKeys.remove(foreignKey);
		}
		else {
			foreignKeyFilterKeys.put(foreignKey, new HashSet<>(keys));
		}
		items().visible().predicate().set(foreignKeyVisiblePredicate);
		items().filter();
	}

	/**
	 * @param foreignKey the foreign key
	 * @return the keys currently used to filter the items of this model by foreign key, an empty collection for none
	 * @see #filterByForeignKey(ForeignKey, Collection)
	 */
	public Collection<Entity.Key> foreignKeyFilterKeys(ForeignKey foreignKey) {
		requireNonNull(foreignKey);
		if (foreignKeyFilterKeys.containsKey(foreignKey)) {
			return unmodifiableCollection(new ArrayList<>(foreignKeyFilterKeys.get(foreignKey)));
		}

		return emptyList();
	}

	/**
	 * Controls whether foreign key filtering should be strict or not.
	 * When the filtering is strict only entities with the correct reference are included, that is,
	 * entities with null values for the given foreign key are filtered.
	 * Non-strict simply means that entities with null references are not filtered.
	 * @return the {@link State} controlling whether foreign key filtering should be strict
	 * @see #filterByForeignKey(ForeignKey, Collection)
	 */
	public State strictForeignKeyFiltering() {
		return strictForeignKeyFiltering;
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyComboBoxModelFactory}
	 */
	public ForeignKeyComboBoxModelFactory foreignKeyComboBoxModel(ForeignKey foreignKey) {
		return new DefaultForeignKeyComboBoxModelFactory(foreignKey);
	}

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyComboBoxModelLinker}
	 */
	public ForeignKeyComboBoxModelLinker foreignKeyComboBoxModelLinker(ForeignKey foreignKey) {
		return new DefaultForeignKeyComboBoxModelLinker(foreignKey);
	}

	/**
	 * Provides a combo box for filtering this combo box instance, either by filter predicate or query condition.
	 */
	public interface ForeignKeyComboBoxModelFactory {

		/**
		 * Returns a combo box model for selecting a foreign key value for filtering this model.
		 * @return a combo box model for selecting a filtering value for this combo box model
		 * @see #foreignKeyComboBoxModelLinker(ForeignKey)
		 */
		EntityComboBoxModel filter();

		/**
		 * Returns a combo box model for selecting a foreign key value for using as a query condition in this model.
		 * Note that each time the selection changes in the resulting model this model is refreshed.
		 * @return a combo box model for selecting a condition query value for this combo box model
		 * @see #foreignKeyComboBoxModelLinker(ForeignKey)
		 */
		EntityComboBoxModel condition();
	}

	/**
	 * Links a given combo box model representing master entities to this combo box model
	 * so that selection in the master model filters this model, either filter predicate or query condition
	 */
	public interface ForeignKeyComboBoxModelLinker {

		/**
		 * Links the given foreign key combo box model via filter predicate
		 * @param foreignKeyModel the combo box model to link
		 */
		void filter(EntityComboBoxModel foreignKeyModel);

		/**
		 * Links the given foreign key combo box model via query condition
		 * @param foreignKeyModel the combo box model to link
		 */
		void condition(EntityComboBoxModel foreignKeyModel);
	}

	/**
	 * Creates a {@link Value} linked to the selected entity via the value of the given attribute.
	 * @param <T> the attribute type
	 * @param attribute the attribute
	 * @return a {@link Value} for selecting items by attribute value
	 */
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

	/**
	 * @param entityType the type of the entity this combo box model should represent
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityComboBoxModel} instance
	 */
	public static EntityComboBoxModel entityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new EntityComboBoxModel(entityType, connectionProvider);
	}

	/**
	 * Retrieves the entities to present in this EntityComboBoxModel, taking into account
	 * the condition supplier ({@link #condition()}) as well as the
	 * select attributes ({@link #attributes()}) and order by clause ({@link #orderBy()}.
	 * @return the entities to present in this EntityComboBoxModel
	 * @see #condition()
	 * @see #attributes()
	 * @see #orderBy()
	 */
	private Collection<Entity> performQuery() {
		try {
			return connectionProvider.connection().select(where(conditionSupplier.get().get())
							.attributes(attributes.get())
							.orderBy(orderBy.get())
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

	private final class DefaultForeignKeyComboBoxModelFactory implements ForeignKeyComboBoxModelFactory {

		private final ForeignKey foreignKey;

		private DefaultForeignKeyComboBoxModelFactory(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public EntityComboBoxModel filter() {
			return createForeignKeyComboBoxModel(foreignKey, true);
		}

		@Override
		public EntityComboBoxModel condition() {
			return createForeignKeyComboBoxModel(foreignKey, false);
		}

		private EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey, boolean filter) {
			entities.definition(entityType).foreignKeys().definition(foreignKey);
			EntityComboBoxModel foreignKeyModel = new EntityComboBoxModel(foreignKey.referencedType(), connectionProvider);
			foreignKeyModel.setNullCaption(FilterComboBoxModel.NULL_CAPTION.get());
			foreignKeyModel.refresh();
			ForeignKeyComboBoxModelLinker modelLinker = foreignKeyComboBoxModelLinker(foreignKey);
			if (filter) {
				modelLinker.filter(foreignKeyModel);
			}
			else {
				modelLinker.condition(foreignKeyModel);
			}

			return foreignKeyModel;
		}
	}

	private final class DefaultForeignKeyComboBoxModelLinker implements ForeignKeyComboBoxModelLinker {

		private final ForeignKey foreignKey;

		private DefaultForeignKeyComboBoxModelLinker(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public void filter(EntityComboBoxModel foreignKeyModel) {
			linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, true);
		}

		@Override
		public void condition(EntityComboBoxModel foreignKeyModel) {
			linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, false);
		}

		private void linkForeignKeyComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel, boolean filter) {
			entities.definition(entityType).foreignKeys().definition(foreignKey);
			if (!foreignKey.referencedType().equals(foreignKeyModel.entityType())) {
				throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.entityType()
								+ ", should be: " + foreignKey.referencedType());
			}
			//if foreign key filter keys have been set previously, initialize with one of those
			Collection<Entity.Key> filterKeys = foreignKeyFilterKeys(foreignKey);
			if (!filterKeys.isEmpty()) {
				foreignKeyModel.select(filterKeys.iterator().next());
			}
			if (filter) {
				linkFilter(foreignKey, foreignKeyModel);
			}
			else {
				linkCondition(foreignKey, foreignKeyModel);
			}
			selection().item().addConsumer(selected -> {
				if (selected != null && !selected.isNull(foreignKey)) {
					foreignKeyModel.select(selected.key(foreignKey));
				}
			});
			refresher().success().addListener(foreignKeyModel::refresh);
		}

		private void linkFilter(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
			Predicate<Entity> filterAllCondition = item -> false;
			if (strictForeignKeyFiltering.get()) {
				items().visible().predicate().set(filterAllCondition);
			}
			foreignKeyModel.selection().item().addConsumer(selected -> {
				if (selected == null && strictForeignKeyFiltering.get()) {
					items().visible().predicate().set(filterAllCondition);
				}
				else {
					filterByForeignKey(foreignKey, selected == null ? emptyList() : singletonList(selected.primaryKey()));
				}
			});
		}

		private void linkCondition(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
			Consumer<Entity> consumer = selected -> {
				conditionSupplier.set(() -> foreignKey.equalTo(selected));
				refresh();
			};
			foreignKeyModel.selection().item().addConsumer(consumer);
			//initialize
			consumer.accept(selection().value());
		}
	}

	private final class AttributeValidator implements Value.Validator<Set<Attribute<?>>> {

		@Override
		public void validate(Set<Attribute<?>> attributes) {
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (!attribute.entityType().equals(entityType)) {
					throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType);
				}
			}
		}
	}

	private final class ItemValidator implements Predicate<Entity> {

		@Override
		public boolean test(Entity entity) {
			return entity.entityType().equals(entityType);
		}
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

	private final class HandleEditEventsChanged implements Consumer<Boolean> {

		@Override
		public void accept(Boolean handleEditEvents) {
			if (handleEditEvents) {
				addEditListeners();
			}
			else {
				removeEditListeners();
			}
		}

		private void addEditListeners() {
			EntityEditEvents.insertObserver(entityType).addWeakConsumer(insertListener);
			EntityEditEvents.updateObserver(entityType).addWeakConsumer(updateListener);
			EntityEditEvents.deleteObserver(entityType).addWeakConsumer(deleteListener);
		}

		private void removeEditListeners() {
			EntityEditEvents.insertObserver(entityType).removeWeakConsumer(insertListener);
			EntityEditEvents.updateObserver(entityType).removeWeakConsumer(updateListener);
			EntityEditEvents.deleteObserver(entityType).removeWeakConsumer(deleteListener);
		}
	}

	private final class ForeignKeyVisiblePredicate implements Predicate<Entity> {

		@Override
		public boolean test(Entity item) {
			for (Map.Entry<ForeignKey, Set<Entity.Key>> entry : foreignKeyFilterKeys.entrySet()) {
				Entity.Key key = item.key(entry.getKey());
				if (key == null) {
					return !strictForeignKeyFiltering.get();
				}
				if (!entry.getValue().contains(key)) {
					return false;
				}
			}

			return true;
		}
	}

	private final class DefaultConditionSupplier implements Supplier<Condition> {

		@Override
		public Condition get() {
			return Condition.all(entityType);
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
}